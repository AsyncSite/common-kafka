package com.asyncsite.kafka.appender;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Custom Logback appender that publishes ERROR-level log events to Kafka.
 * This catches ALL log.error() calls automatically, including those inside catch blocks
 * that GlobalExceptionHandler cannot reach.
 *
 * <p>Configuration in logback-spring.xml:
 * <pre>{@code
 * <appender name="KAFKA_ERROR" class="com.asyncsite.kafka.appender.KafkaErrorLogAppender">
 *     <bootstrapServers>localhost:9092</bootstrapServers>
 *     <topic>asyncsite.error.events</topic>
 *     <serviceName>my-service</serviceName>
 * </appender>
 * }</pre>
 *
 * <p>Key design decisions:
 * <ul>
 *   <li>Uses raw KafkaProducer (not Spring KafkaTemplate) for logback compatibility</li>
 *   <li>Self-loop prevention: ignores errors from Kafka/appender packages</li>
 *   <li>Rate limiting: max 10 messages per 60 seconds per unique error key</li>
 *   <li>Async send: fire-and-forget to minimize performance impact</li>
 *   <li>Graceful degradation: logs warning on Kafka failure, never throws</li>
 * </ul>
 */
public class KafkaErrorLogAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

    private static final int MAX_STACK_TRACE_LENGTH = 500;
    private static final int MAX_MESSAGE_LENGTH = 1000;
    private static final long RATE_LIMIT_WINDOW_MS = 60_000;
    private static final int RATE_LIMIT_MAX_PER_KEY = 10;
    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneId.of("Asia/Seoul"));

    // Configurable properties (set via logback XML)
    private String bootstrapServers = "localhost:9092";
    private String topic = "asyncsite.error.events";
    private String serviceName = "unknown-service";

    private volatile KafkaProducer<String, String> producer;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final AtomicBoolean failed = new AtomicBoolean(false);

    // Rate limiting: key -> (count, windowStart)
    private final ConcurrentHashMap<String, long[]> rateLimitMap = new ConcurrentHashMap<>();
    private final AtomicLong lastCleanup = new AtomicLong(System.currentTimeMillis());

    // Packages to ignore to prevent self-loop
    private static final String[] IGNORED_LOGGER_PREFIXES = {
        "org.apache.kafka",
        "com.asyncsite.kafka.appender",
        "org.springframework.kafka",
        "kafka.producer",
        "kafka.common",
        "kafka.network",
        "kafka.server"
    };

    // Setters for logback XML configuration
    public void setBootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    @Override
    public void start() {
        super.start();
        // Lazy init - producer created on first error to avoid startup ordering issues
    }

    @Override
    public void stop() {
        super.stop();
        if (producer != null) {
            try {
                producer.close(java.time.Duration.ofSeconds(5));
            } catch (Exception e) {
                addWarn("Failed to close Kafka producer", e);
            }
        }
    }

    @Override
    protected void append(ILoggingEvent event) {
        // Only process ERROR level
        if (event.getLevel().toInt() < Level.ERROR_INT) {
            return;
        }

        // Self-loop prevention
        String loggerName = event.getLoggerName();
        for (String prefix : IGNORED_LOGGER_PREFIXES) {
            if (loggerName.startsWith(prefix)) {
                return;
            }
        }

        // Rate limiting
        String rateLimitKey = loggerName + ":" + truncate(event.getMessage(), 100);
        if (isRateLimited(rateLimitKey)) {
            return;
        }

        // Lazy init producer
        if (!initProducerIfNeeded()) {
            return;
        }

        try {
            String json = buildJsonPayload(event);
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, serviceName, json);

            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    // Don't use addError to avoid potential loops
                    addWarn("Kafka error alert send failed: " + exception.getMessage());
                }
            });
        } catch (Exception e) {
            addWarn("Failed to send error event to Kafka: " + e.getMessage());
        }
    }

    private boolean initProducerIfNeeded() {
        if (initialized.get()) {
            return !failed.get();
        }

        synchronized (this) {
            if (initialized.get()) {
                return !failed.get();
            }

            try {
                Properties props = new Properties();
                props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
                props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
                props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
                props.put(ProducerConfig.ACKS_CONFIG, "1");
                props.put(ProducerConfig.RETRIES_CONFIG, 1);
                props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 3000);
                props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);
                props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 10000);
                props.put(ProducerConfig.LINGER_MS_CONFIG, 100);
                props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
                props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 1048576); // 1MB
                props.put(ProducerConfig.CLIENT_ID_CONFIG, serviceName + "-error-log-appender");

                producer = new KafkaProducer<>(props);
                initialized.set(true);
                addInfo("Kafka error log appender initialized for service: " + serviceName);
                return true;
            } catch (Exception e) {
                addWarn("Failed to initialize Kafka producer for error logging: " + e.getMessage());
                failed.set(true);
                initialized.set(true);
                return false;
            }
        }
    }

    private boolean isRateLimited(String key) {
        long now = System.currentTimeMillis();

        // Periodic cleanup of old entries
        if (now - lastCleanup.get() > RATE_LIMIT_WINDOW_MS * 2) {
            if (lastCleanup.compareAndSet(lastCleanup.get(), now)) {
                rateLimitMap.entrySet().removeIf(e -> now - e.getValue()[1] > RATE_LIMIT_WINDOW_MS);
            }
        }

        return rateLimitMap.compute(key, (k, v) -> {
            if (v == null || now - v[1] > RATE_LIMIT_WINDOW_MS) {
                return new long[]{1, now};
            }
            v[0]++;
            return v;
        })[0] > RATE_LIMIT_MAX_PER_KEY;
    }

    private String buildJsonPayload(ILoggingEvent event) {
        StringBuilder sb = new StringBuilder(512);
        sb.append("{");
        appendJsonField(sb, "service", serviceName);
        sb.append(",");
        appendJsonField(sb, "errorType", extractErrorType(event));
        sb.append(",");
        appendJsonField(sb, "message", truncate(event.getFormattedMessage(), MAX_MESSAGE_LENGTH));
        sb.append(",");
        appendJsonField(sb, "stackTrace", extractStackTrace(event));
        sb.append(",");
        appendJsonField(sb, "loggerName", event.getLoggerName());
        sb.append(",");
        appendJsonField(sb, "threadName", event.getThreadName());
        sb.append(",");
        // requestPath/requestMethod/httpStatus left empty for log-sourced errors
        appendJsonField(sb, "requestPath", "");
        sb.append(",");
        appendJsonField(sb, "requestMethod", "");
        sb.append(",");
        sb.append("\"httpStatus\":0");
        sb.append(",");
        appendJsonField(sb, "timestamp", TIMESTAMP_FMT.format(Instant.ofEpochMilli(event.getTimeStamp())));
        sb.append(",");
        appendJsonField(sb, "source", "logback-appender");
        sb.append("}");
        return sb.toString();
    }

    private String extractErrorType(ILoggingEvent event) {
        IThrowableProxy throwableProxy = event.getThrowableProxy();
        if (throwableProxy != null) {
            String className = throwableProxy.getClassName();
            int lastDot = className.lastIndexOf('.');
            return lastDot >= 0 ? className.substring(lastDot + 1) : className;
        }
        // If no exception attached, use the logger class simple name
        String logger = event.getLoggerName();
        int lastDot = logger.lastIndexOf('.');
        return lastDot >= 0 ? logger.substring(lastDot + 1) + ".log" : logger + ".log";
    }

    private String extractStackTrace(ILoggingEvent event) {
        IThrowableProxy throwableProxy = event.getThrowableProxy();
        if (throwableProxy == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(throwableProxy.getClassName())
          .append(": ")
          .append(throwableProxy.getMessage())
          .append("\n");

        StackTraceElementProxy[] stepArray = throwableProxy.getStackTraceElementProxyArray();
        if (stepArray != null) {
            for (StackTraceElementProxy step : stepArray) {
                if (sb.length() + step.toString().length() + 5 > MAX_STACK_TRACE_LENGTH) {
                    sb.append("...");
                    break;
                }
                sb.append("\tat ").append(step).append("\n");
            }
        }

        return sb.toString();
    }

    private void appendJsonField(StringBuilder sb, String key, String value) {
        sb.append("\"").append(key).append("\":\"").append(escapeJson(value)).append("\"");
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return "";
        return value.length() <= maxLength ? value : value.substring(0, maxLength) + "...";
    }
}

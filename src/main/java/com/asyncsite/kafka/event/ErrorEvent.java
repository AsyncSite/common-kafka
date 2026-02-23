package com.asyncsite.kafka.event;

import java.io.Serializable;

/**
 * Error event DTO for cross-service error notification via Kafka.
 * Published by services via two sources:
 * <ul>
 *   <li>GlobalExceptionHandler: catches HTTP 500 errors (has requestPath/httpStatus)</li>
 *   <li>KafkaErrorLogAppender: catches ALL log.error() calls (has loggerName/threadName)</li>
 * </ul>
 * Consumed by noti-service ErrorEventListener to send Discord alerts.
 *
 * <p>This is a plain Java POJO (no Lombok, no Kotlin dependencies)
 * so it can be used by any service regardless of their dependency setup.
 * Services that do not depend on common-kafka can define their own
 * identical DTO and publish to the same topic.</p>
 */
public class ErrorEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String TOPIC = "asyncsite.error.events";

    private String service;
    private String errorType;
    private String message;
    private String stackTrace;
    private String requestPath;
    private String requestMethod;
    private int httpStatus;
    private String timestamp;
    // New fields for log-sourced errors (from KafkaErrorLogAppender)
    private String loggerName;
    private String threadName;
    private String source; // "global-exception-handler" or "logback-appender"

    public ErrorEvent() {
    }

    private ErrorEvent(Builder builder) {
        this.service = builder.service;
        this.errorType = builder.errorType;
        this.message = builder.message;
        this.stackTrace = builder.stackTrace;
        this.requestPath = builder.requestPath;
        this.requestMethod = builder.requestMethod;
        this.httpStatus = builder.httpStatus;
        this.timestamp = builder.timestamp;
        this.loggerName = builder.loggerName;
        this.threadName = builder.threadName;
        this.source = builder.source;
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters

    public String getService() {
        return service;
    }

    public String getErrorType() {
        return errorType;
    }

    public String getMessage() {
        return message;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public String getRequestPath() {
        return requestPath;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getLoggerName() {
        return loggerName;
    }

    public String getThreadName() {
        return threadName;
    }

    public String getSource() {
        return source;
    }

    /**
     * Returns true if this error was captured from a log.error() call via KafkaErrorLogAppender
     * rather than from GlobalExceptionHandler.
     */
    public boolean isLogSourced() {
        return "logback-appender".equals(source);
    }

    // Setters (needed for Jackson deserialization)

    public void setService(String service) {
        this.service = service;
    }

    public void setErrorType(String errorType) {
        this.errorType = errorType;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public void setRequestPath(String requestPath) {
        this.requestPath = requestPath;
    }

    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }

    public void setHttpStatus(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public void setLoggerName(String loggerName) {
        this.loggerName = loggerName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    public void setSource(String source) {
        this.source = source;
    }

    @Override
    public String toString() {
        return "ErrorEvent{" +
                "service='" + service + '\'' +
                ", errorType='" + errorType + '\'' +
                ", message='" + message + '\'' +
                ", requestPath='" + requestPath + '\'' +
                ", httpStatus=" + httpStatus +
                ", timestamp='" + timestamp + '\'' +
                '}';
    }

    /**
     * Truncates a stack trace to the specified maximum length.
     * Utility method for producers to use before publishing.
     */
    public static String truncateStackTrace(Throwable throwable, int maxLength) {
        if (throwable == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(throwable.toString()).append("\n");
        StackTraceElement[] elements = throwable.getStackTrace();
        for (StackTraceElement element : elements) {
            if (sb.length() + element.toString().length() + 5 > maxLength) {
                sb.append("...");
                break;
            }
            sb.append("\tat ").append(element).append("\n");
        }
        return sb.toString();
    }

    public static class Builder {
        private String service;
        private String errorType;
        private String message;
        private String stackTrace;
        private String requestPath;
        private String requestMethod;
        private int httpStatus;
        private String timestamp;
        private String loggerName;
        private String threadName;
        private String source;

        public Builder service(String service) {
            this.service = service;
            return this;
        }

        public Builder errorType(String errorType) {
            this.errorType = errorType;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder stackTrace(String stackTrace) {
            this.stackTrace = stackTrace;
            return this;
        }

        public Builder requestPath(String requestPath) {
            this.requestPath = requestPath;
            return this;
        }

        public Builder requestMethod(String requestMethod) {
            this.requestMethod = requestMethod;
            return this;
        }

        public Builder httpStatus(int httpStatus) {
            this.httpStatus = httpStatus;
            return this;
        }

        public Builder timestamp(String timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder loggerName(String loggerName) {
            this.loggerName = loggerName;
            return this;
        }

        public Builder threadName(String threadName) {
            this.threadName = threadName;
            return this;
        }

        public Builder source(String source) {
            this.source = source;
            return this;
        }

        public ErrorEvent build() {
            return new ErrorEvent(this);
        }
    }
}

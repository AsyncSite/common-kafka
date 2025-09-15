package com.asyncsite.coreplatform.kafka.correlation

import mu.KotlinLogging
import org.apache.kafka.clients.producer.ProducerInterceptor
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.header.internals.RecordHeader
import org.slf4j.MDC
import java.util.UUID

private val log = KotlinLogging.logger {}

/**
 * Kafka producer interceptor that automatically adds correlation ID to message headers.
 * This ensures correlation ID is propagated through Kafka messages for distributed tracing.
 */
class KafkaCorrelationIdProducerInterceptor : ProducerInterceptor<String, Any> {
    
    companion object {
        const val CORRELATION_ID_HEADER = "X-Correlation-Id"
        const val CAUSATION_ID_HEADER = "X-Causation-Id"
        const val USER_ID_HEADER = "X-User-Id"
        const val SOURCE_SERVICE_HEADER = "X-Source-Service"
        const val TIMESTAMP_HEADER = "X-Timestamp"
    }
    
    /**
     * Called when the interceptor is configured.
     * Can be used to initialize resources.
     */
    override fun configure(configs: MutableMap<String, *>?) {
        log.debug { "KafkaCorrelationIdProducerInterceptor configured with: $configs" }
    }
    
    /**
     * Called before the record is sent to Kafka.
     * Adds correlation ID and other metadata to record headers.
     */
    override fun onSend(record: ProducerRecord<String, Any>): ProducerRecord<String, Any> {
        // Extract correlation ID from MDC or generate new one
        val correlationId = MDC.get("correlationId") ?: UUID.randomUUID().toString()
        
        // Add correlation ID to headers
        record.headers().add(RecordHeader(CORRELATION_ID_HEADER, correlationId.toByteArray()))
        
        // Add causation ID if this event was caused by another event
        MDC.get("causationId")?.let { causationId ->
            record.headers().add(RecordHeader(CAUSATION_ID_HEADER, causationId.toByteArray()))
        }
        
        // Add user context if available
        MDC.get("userId")?.let { userId ->
            record.headers().add(RecordHeader(USER_ID_HEADER, userId.toByteArray()))
        }
        
        // Add source service identifier
        val sourceService = System.getProperty("spring.application.name") ?: "unknown"
        record.headers().add(RecordHeader(SOURCE_SERVICE_HEADER, sourceService.toByteArray()))
        
        // Add timestamp
        val timestamp = System.currentTimeMillis().toString()
        record.headers().add(RecordHeader(TIMESTAMP_HEADER, timestamp.toByteArray()))
        
        log.trace { 
            "Added headers to Kafka record - Topic: ${record.topic()}, " +
            "Key: ${record.key()}, CorrelationId: $correlationId" 
        }
        
        return record
    }
    
    /**
     * Called when the record has been acknowledged by the server.
     * Can be used for metrics or logging.
     */
    override fun onAcknowledgement(metadata: RecordMetadata?, exception: Exception?) {
        if (exception != null) {
            log.error(exception) { 
                "Failed to send record to Kafka - Topic: ${metadata?.topic()}, " +
                "Partition: ${metadata?.partition()}, Offset: ${metadata?.offset()}" 
            }
        } else if (metadata != null) {
            log.trace { 
                "Record acknowledged - Topic: ${metadata.topic()}, " +
                "Partition: ${metadata.partition()}, Offset: ${metadata.offset()}" 
            }
        }
    }
    
    /**
     * Called when the interceptor is closed.
     * Should clean up any resources.
     */
    override fun close() {
        log.debug { "KafkaCorrelationIdProducerInterceptor closed" }
    }
}
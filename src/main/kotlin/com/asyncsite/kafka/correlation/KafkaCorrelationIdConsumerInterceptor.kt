package com.asyncsite.kafka.correlation

import mu.KotlinLogging
import org.apache.kafka.clients.consumer.ConsumerInterceptor
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import org.slf4j.MDC

private val log = KotlinLogging.logger {}

/**
 * Kafka consumer interceptor that extracts correlation ID from message headers
 * and sets it in MDC for logging context.
 */
class KafkaCorrelationIdConsumerInterceptor : ConsumerInterceptor<String, Any> {
    
    companion object {
        const val CORRELATION_ID_HEADER = "X-Correlation-Id"
        const val CAUSATION_ID_HEADER = "X-Causation-Id"
        const val USER_ID_HEADER = "X-User-Id"
        const val SOURCE_SERVICE_HEADER = "X-Source-Service"
    }
    
    /**
     * Called when the interceptor is configured.
     */
    override fun configure(configs: MutableMap<String, *>?) {
        log.debug { "KafkaCorrelationIdConsumerInterceptor configured" }
    }
    
    /**
     * Called before the records are returned to the consumer.
     * Extracts correlation ID from the first record for logging context.
     */
    override fun onConsume(records: ConsumerRecords<String, Any>): ConsumerRecords<String, Any> {
        if (!records.isEmpty) {
            // Get the first record to extract correlation ID
            val firstRecord = records.first()
            
            // Extract correlation ID from headers
            val correlationIdHeader = firstRecord.headers().lastHeader(CORRELATION_ID_HEADER)
            if (correlationIdHeader != null) {
                val correlationId = String(correlationIdHeader.value())
                MDC.put("correlationId", correlationId)
                
                log.trace { 
                    "Set correlation ID from Kafka message: $correlationId, " +
                    "Topic: ${firstRecord.topic()}, Partition: ${firstRecord.partition()}" 
                }
            }
            
            // Extract other headers if needed
            firstRecord.headers().lastHeader(CAUSATION_ID_HEADER)?.let {
                MDC.put("causationId", String(it.value()))
            }
            
            firstRecord.headers().lastHeader(USER_ID_HEADER)?.let {
                MDC.put("userId", String(it.value()))
            }
            
            firstRecord.headers().lastHeader(SOURCE_SERVICE_HEADER)?.let {
                MDC.put("sourceService", String(it.value()))
            }
            
            log.debug { 
                "Processing ${records.count()} records from topics: ${records.partitions().map { it.topic() }.distinct()}" 
            }
        }
        
        return records
    }
    
    /**
     * Called when offsets are committed.
     * Can be used for metrics or cleanup.
     */
    override fun onCommit(offsets: MutableMap<TopicPartition, OffsetAndMetadata>?) {
        if (offsets != null && offsets.isNotEmpty()) {
            log.trace { 
                "Committed offsets for ${offsets.size} partitions" 
            }
            
            // Clear MDC after commit
            MDC.remove("correlationId")
            MDC.remove("causationId")
            MDC.remove("userId")
            MDC.remove("sourceService")
        }
    }
    
    /**
     * Called when the interceptor is closed.
     */
    override fun close() {
        log.debug { "KafkaCorrelationIdConsumerInterceptor closed" }
        
        // Final cleanup of MDC
        MDC.clear()
    }
}
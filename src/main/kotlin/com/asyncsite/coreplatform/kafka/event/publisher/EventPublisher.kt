package com.asyncsite.coreplatform.kafka.event.publisher

import com.asyncsite.coreplatform.kafka.event.BaseEvent
import com.asyncsite.coreplatform.kafka.event.EventEnvelope
import com.asyncsite.coreplatform.kafka.event.EventMetadata
import mu.KotlinLogging
import org.slf4j.MDC
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeader
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CompletableFuture

private val log = KotlinLogging.logger {}

/**
 * Standard event publisher for Kafka.
 * Handles event publishing with automatic correlation ID propagation and error handling.
 */
@Component
class EventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {
    
    /**
     * Publishes an event to the specified topic asynchronously.
     * Automatically adds correlation ID from MDC if available.
     * 
     * @param event The event to publish
     * @param topic The Kafka topic to publish to
     * @return CompletableFuture with the send result
     */
    fun publishAsync(event: BaseEvent, topic: String): CompletableFuture<SendResult<String, Any>> {
        val correlationId = MDC.get("correlationId") ?: UUID.randomUUID().toString()
        
        val metadata = EventMetadata(
            correlationId = correlationId,
            source = this::class.java.simpleName,
            publishedAt = Instant.now()
        )
        
        return publishAsyncWithMetadata(event, topic, metadata)
    }
    
    /**
     * Publishes an event with a specific partition key asynchronously.
     * Useful for ensuring events for the same aggregate go to the same partition.
     * 
     * @param event The event to publish
     * @param topic The Kafka topic to publish to
     * @param partitionKey The key to use for partition selection
     * @return CompletableFuture with the send result
     */
    fun publishAsyncWithKey(
        event: BaseEvent, 
        topic: String, 
        partitionKey: String
    ): CompletableFuture<SendResult<String, Any>> {
        val correlationId = MDC.get("correlationId") ?: UUID.randomUUID().toString()
        
        log.debug { 
            "Publishing event: ${event.eventType} to topic: $topic " +
            "with key: $partitionKey and correlationId: $correlationId" 
        }
        
        // Create ProducerRecord with headers for type information
        val producerRecord = ProducerRecord<String, Any>(
            topic,
            null, // partition (let Kafka decide based on key)
            partitionKey,
            event
        )
        
        // Add custom headers for event metadata (no type headers for microservices independence)
        producerRecord.headers().apply {
            // Custom headers for event metadata
            add(RecordHeader("correlationId", correlationId.toByteArray()))
            add(RecordHeader("eventType", event.eventType.toByteArray()))
            add(RecordHeader("eventId", event.eventId.toByteArray()))
        }
        
        return kafkaTemplate.send(producerRecord).also { future ->
            future.whenComplete { result, ex ->
                if (ex != null) {
                    log.error(ex) { 
                        "Failed to publish event: ${event.eventType} to topic: $topic" 
                    }
                    handlePublishFailure(event, topic, ex)
                } else {
                    log.debug { 
                        "Event published successfully: ${event.eventType} " +
                        "to partition: ${result.recordMetadata.partition()} " +
                        "with offset: ${result.recordMetadata.offset()}" 
                    }
                }
            }
        }
    }
    
    /**
     * Publishes an event with custom metadata asynchronously.
     * 
     * @param event The event to publish
     * @param topic The Kafka topic to publish to
     * @param metadata Custom metadata for the event
     * @return CompletableFuture with the send result
     */
    fun publishAsyncWithMetadata(
        event: BaseEvent,
        topic: String,
        metadata: EventMetadata
    ): CompletableFuture<SendResult<String, Any>> {
        val envelope = EventEnvelope(
            event = event,
            metadata = metadata,
            topic = topic
        )
        
        return publishAsyncEnvelope(envelope)
    }
    
    /**
     * Publishes an event envelope asynchronously.
     * This is the core publishing method that others delegate to.
     * 
     * @param envelope The event envelope to publish
     * @return CompletableFuture with the send result
     */
    fun publishAsyncEnvelope(envelope: EventEnvelope<*>): CompletableFuture<SendResult<String, Any>> {
        val topic = envelope.topic 
            ?: throw IllegalArgumentException("Topic must be specified in envelope")
        
        val partitionKey = envelope.metadata.partitionKey ?: envelope.eventId
        
        log.debug { 
            "Publishing envelope: ${envelope.eventType} to topic: $topic " +
            "with correlationId: ${envelope.metadata.correlationId}" 
        }
        
        // Create ProducerRecord with headers for type information
        val producerRecord = ProducerRecord<String, Any>(
            topic,
            null, // partition (let Kafka decide based on key)
            partitionKey,
            envelope.event
        )
        
        // Add custom headers for event metadata (no type headers for microservices independence)
        producerRecord.headers().apply {
            // Custom headers for event metadata
            envelope.metadata.correlationId?.let {
                add(RecordHeader("correlationId", it.toByteArray()))
            } ?: log.warn { "No correlationId found in MDC for event: ${envelope.eventType}" }
            
            add(RecordHeader("eventType", envelope.eventType.toByteArray()))
            add(RecordHeader("eventId", envelope.eventId.toByteArray()))
        }
        
        log.debug { 
            "Publishing event with headers - EventType: ${envelope.eventType}, " +
            "EventId: ${envelope.eventId}, CorrelationId: ${envelope.metadata.correlationId}" 
        }
        
        // Send the record with headers
        val future = kafkaTemplate.send(producerRecord)
        
        return future.also { resultFuture ->
            resultFuture.whenComplete { result, ex ->
                if (ex != null) {
                    log.error(ex) { 
                        "Failed to publish envelope: ${envelope.eventType} to topic: $topic" 
                    }
                    handlePublishFailure(envelope.event, topic, ex)
                } else {
                    log.debug { 
                        "Envelope published successfully: ${envelope.eventType} " +
                        "to partition: ${result.recordMetadata.partition()} " +
                        "with offset: ${result.recordMetadata.offset()}" 
                    }
                }
            }
        }
    }
    
    /**
     * Publishes multiple events as a batch.
     * All events go to the same topic.
     * 
     * @param events List of events to publish
     * @param topic The Kafka topic to publish to
     * @return List of CompletableFutures for each event
     */
    fun publishBatch(
        events: List<BaseEvent>, 
        topic: String
    ): List<CompletableFuture<SendResult<String, Any>>> {
        log.debug { "Publishing batch of ${events.size} events to topic: $topic" }
        
        return events.map { event ->
            publishAsync(event, topic)
        }
    }
    
    /**
     * Publishes an event and waits for the result synchronously.
     * Use with caution as it blocks the thread.
     * 
     * @param event The event to publish
     * @param topic The Kafka topic to publish to
     * @param timeoutMs Maximum time to wait in milliseconds
     * @return SendResult if successful
     * @throws Exception if publishing fails or times out
     */
    fun publishSync(
        event: BaseEvent, 
        topic: String, 
        timeoutMs: Long = 5000
    ): SendResult<String, Any> {
        val future = publishAsync(event, topic)
        
        return try {
            future.get(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS)
        } catch (e: Exception) {
            log.error(e) { 
                "Synchronous publish failed for event: ${event.eventType} to topic: $topic" 
            }
            throw e
        }
    }
    
    /**
     * Handles publish failures.
     * Can be overridden by subclasses for custom error handling.
     */
    protected open fun handlePublishFailure(event: BaseEvent, topic: String, exception: Throwable) {
        // Default implementation just logs
        // Could be extended to send to DLQ, alert monitoring, etc.
        log.error(exception) { 
            "Event publish failed - EventType: ${event.eventType}, " +
            "EventId: ${event.eventId}, Topic: $topic" 
        }
    }
}
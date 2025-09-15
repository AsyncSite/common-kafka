package com.asyncsite.coreplatform.kafka.event

import java.time.Instant

/**
 * Wrapper for events with metadata.
 * Combines the business event with its metadata for processing.
 * 
 * @param T The type of the event, must extend BaseEvent
 */
data class EventEnvelope<T : BaseEvent>(
    /**
     * The actual business event
     */
    val event: T,
    
    /**
     * Metadata associated with the event
     */
    val metadata: EventMetadata = EventMetadata(),
    
    /**
     * Topic where this event should be published
     */
    val topic: String? = null,
    
    /**
     * Timestamp when the envelope was created
     */
    val createdAt: Instant = Instant.now()
) {
    /**
     * Gets the event ID from the wrapped event
     */
    val eventId: String
        get() = event.eventId
    
    /**
     * Gets the event type from the wrapped event
     */
    val eventType: String
        get() = event.eventType
    
    /**
     * Creates a new envelope with updated metadata
     */
    fun withMetadata(metadata: EventMetadata): EventEnvelope<T> {
        return copy(metadata = metadata)
    }
    
    /**
     * Creates a new envelope with additional metadata headers
     */
    fun withHeaders(headers: Map<String, String>): EventEnvelope<T> {
        return copy(metadata = metadata.withHeaders(headers))
    }
    
    /**
     * Creates a new envelope with correlation ID
     */
    fun withCorrelationId(correlationId: String): EventEnvelope<T> {
        return copy(metadata = metadata.withCorrelationId(correlationId))
    }
    
    /**
     * Creates a new envelope with topic
     */
    fun withTopic(topic: String): EventEnvelope<T> {
        return copy(topic = topic)
    }
    
    companion object {
        /**
         * Creates an envelope from an event with minimal metadata
         */
        fun <T : BaseEvent> of(event: T): EventEnvelope<T> {
            return EventEnvelope(event)
        }
        
        /**
         * Creates an envelope from an event with correlation ID
         */
        fun <T : BaseEvent> of(event: T, correlationId: String): EventEnvelope<T> {
            return EventEnvelope(
                event = event,
                metadata = EventMetadata(correlationId = correlationId)
            )
        }
        
        /**
         * Creates an envelope from an event with full metadata
         */
        fun <T : BaseEvent> of(event: T, metadata: EventMetadata): EventEnvelope<T> {
            return EventEnvelope(event = event, metadata = metadata)
        }
    }
}
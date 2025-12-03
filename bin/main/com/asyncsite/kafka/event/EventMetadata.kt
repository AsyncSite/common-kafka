package com.asyncsite.kafka.event

import java.time.Instant

/**
 * Metadata that can be attached to any event.
 * Contains contextual information about the event that is not part of the business data.
 */
data class EventMetadata(
    /**
     * Correlation ID for request tracing across services
     */
    val correlationId: String? = null,
    
    /**
     * ID of the event that caused this event (for event chains)
     */
    val causationId: String? = null,
    
    /**
     * User ID who triggered the event
     */
    val userId: String? = null,
    
    /**
     * Service or component that generated the event
     */
    val source: String? = null,
    
    /**
     * Additional headers for custom metadata
     */
    val headers: Map<String, String> = emptyMap(),
    
    /**
     * Timestamp when the event was published (may differ from occurredAt)
     */
    val publishedAt: Instant? = null,
    
    /**
     * Partition key for Kafka (if different from default)
     */
    val partitionKey: String? = null
) {
    companion object {
        /**
         * Standard header names
         */
        const val CORRELATION_ID_HEADER = "X-Correlation-Id"
        const val CAUSATION_ID_HEADER = "X-Causation-Id"
        const val USER_ID_HEADER = "X-User-Id"
        const val USER_EMAIL_HEADER = "X-User-Email"
        const val USER_ROLES_HEADER = "X-User-Roles"
        const val SOURCE_HEADER = "X-Event-Source"
        const val PUBLISHED_AT_HEADER = "X-Published-At"
        
        /**
         * Creates metadata from standard headers
         */
        fun fromHeaders(headers: Map<String, String>): EventMetadata {
            return EventMetadata(
                correlationId = headers[CORRELATION_ID_HEADER],
                causationId = headers[CAUSATION_ID_HEADER],
                userId = headers[USER_ID_HEADER],
                source = headers[SOURCE_HEADER],
                headers = headers
            )
        }
    }
    
    /**
     * Converts metadata to headers map for Kafka
     */
    fun toHeaders(): Map<String, String> {
        val result = mutableMapOf<String, String>()
        
        correlationId?.let { result[CORRELATION_ID_HEADER] = it }
        causationId?.let { result[CAUSATION_ID_HEADER] = it }
        userId?.let { result[USER_ID_HEADER] = it }
        source?.let { result[SOURCE_HEADER] = it }
        publishedAt?.let { result[PUBLISHED_AT_HEADER] = it.toString() }
        
        result.putAll(headers)
        
        return result
    }
    
    /**
     * Creates a new metadata with additional headers
     */
    fun withHeaders(additionalHeaders: Map<String, String>): EventMetadata {
        return copy(headers = headers + additionalHeaders)
    }
    
    /**
     * Creates a new metadata with updated correlation ID
     */
    fun withCorrelationId(correlationId: String): EventMetadata {
        return copy(correlationId = correlationId)
    }
}
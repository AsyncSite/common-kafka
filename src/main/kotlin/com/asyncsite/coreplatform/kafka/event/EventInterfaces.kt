package com.asyncsite.coreplatform.kafka.event

/**
 * Interface for events that carry correlation ID for request tracing.
 * Correlation ID helps track a request across multiple services.
 */
interface CorrelatedEvent {
    val correlationId: String
}

/**
 * Interface for events that carry user context information.
 * Used when the event is triggered by or related to a specific user.
 */
interface UserContextEvent {
    val userId: String?
    val userEmail: String?
    val userRoles: Set<String>?
}

/**
 * Interface for events that can be retried upon failure.
 * Provides configuration for retry behavior.
 */
interface RetryableEvent {
    /**
     * Maximum number of retry attempts
     */
    val maxRetries: Int
        get() = 3
    
    /**
     * Delay between retry attempts in milliseconds
     */
    val retryDelayMs: Long
        get() = 1000
    
    /**
     * Whether exponential backoff should be used for retries
     */
    val useExponentialBackoff: Boolean
        get() = true
}

/**
 * Interface for events that represent aggregate state changes.
 * Used in Event Sourcing pattern.
 */
interface AggregateEvent {
    /**
     * ID of the aggregate that this event belongs to
     */
    val aggregateId: String
    
    /**
     * Type of the aggregate (e.g., "User", "Study", "Payment")
     */
    val aggregateType: String
}

/**
 * Interface for events that can be compensated in Saga pattern.
 * Used for distributed transaction management.
 */
interface CompensatableEvent {
    /**
     * The compensating event type that reverses this event's effect
     */
    val compensatingEventType: String?
    
    /**
     * Whether this event has been compensated
     */
    val isCompensated: Boolean
        get() = false
}
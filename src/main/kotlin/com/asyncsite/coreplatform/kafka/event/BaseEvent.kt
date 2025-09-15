package com.asyncsite.coreplatform.kafka.event

import java.time.Instant
import java.util.UUID

/**
 * Base class for all domain events in the AsyncSite platform.
 * Provides common fields for event tracking and correlation.
 * 
 * All domain events must extend this class to ensure consistent
 * event structure across the platform.
 */
abstract class BaseEvent {
    /**
     * Unique identifier for this event instance
     */
    val eventId: String = UUID.randomUUID().toString()
    
    /**
     * Timestamp when the event occurred
     */
    val occurredAt: Instant = Instant.now()
    
    /**
     * Version of the event schema for backward compatibility
     */
    open val eventVersion: String = "1.0"
    
    /**
     * Type of the event (e.g., "UserRegistered", "StudyCreated")
     * Must be implemented by concrete event classes
     */
    abstract val eventType: String
    
    override fun toString(): String {
        return "${this::class.simpleName}(eventId=$eventId, eventType=$eventType, occurredAt=$occurredAt)"
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BaseEvent) return false
        return eventId == other.eventId
    }
    
    override fun hashCode(): Int {
        return eventId.hashCode()
    }
}
package com.asyncsite.kafka.event

import com.asyncsite.kafka.event.examples.UserRegisteredEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class BaseEventTest {
    
    @Test
    fun `should generate unique eventId for each event`() {
        // Given
        val event1 = UserRegisteredEvent(
            userId = "user1",
            email = "test1@example.com",
            name = "Test User 1"
        )
        val event2 = UserRegisteredEvent(
            userId = "user1",
            email = "test1@example.com",
            name = "Test User 1"
        )
        
        // Then
        assertThat(event1.eventId).isNotEqualTo(event2.eventId)
        assertThat(UUID.fromString(event1.eventId)).isNotNull
        assertThat(UUID.fromString(event2.eventId)).isNotNull
    }
    
    @Test
    fun `should set occurredAt timestamp on creation`() {
        // Given
        val beforeCreation = Instant.now()
        
        // When
        val event = UserRegisteredEvent(
            userId = "user1",
            email = "test@example.com",
            name = "Test User"
        )
        
        val afterCreation = Instant.now()
        
        // Then
        assertThat(event.occurredAt).isAfterOrEqualTo(beforeCreation)
        assertThat(event.occurredAt).isBeforeOrEqualTo(afterCreation)
    }
    
    @Test
    fun `should have correct eventType`() {
        // Given
        val event = UserRegisteredEvent(
            userId = "user1",
            email = "test@example.com",
            name = "Test User"
        )
        
        // Then
        assertThat(event.eventType).isEqualTo("UserRegistered")
    }
    
    @Test
    fun `should have default event version`() {
        // Given
        val event = UserRegisteredEvent(
            userId = "user1",
            email = "test@example.com",
            name = "Test User"
        )
        
        // Then
        assertThat(event.eventVersion).isEqualTo("1.0")
    }
    
    @Test
    fun `should implement equals based on eventId`() {
        // Given
        val event1 = UserRegisteredEvent(
            userId = "user1",
            email = "test@example.com",
            name = "Test User"
        )
        val event2 = UserRegisteredEvent(
            userId = "user1",  // Same data for data class equals
            email = "test@example.com",
            name = "Test User"
        )
        
        // When - Create a copy with same eventId using reflection
        val eventIdField = BaseEvent::class.java.getDeclaredField("eventId")
        eventIdField.isAccessible = true
        eventIdField.set(event2, event1.eventId)
        
        // Then
        assertThat(event1).isEqualTo(event2)
        assertThat(event1.hashCode()).isEqualTo(event2.hashCode())
    }
    
    @Test
    fun `should have meaningful toString`() {
        // Given
        val event = UserRegisteredEvent(
            userId = "user1",
            email = "test@example.com",
            name = "Test User"
        )
        
        // When
        val stringRepresentation = event.toString()
        
        // Then - data class toString includes all fields
        assertThat(stringRepresentation).contains("UserRegisteredEvent")
        assertThat(stringRepresentation).contains("userId=user1")
        assertThat(stringRepresentation).contains("email=test@example.com")
        assertThat(stringRepresentation).contains("name=Test User")
    }
}
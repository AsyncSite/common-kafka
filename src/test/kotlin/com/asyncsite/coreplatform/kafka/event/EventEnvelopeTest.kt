package com.asyncsite.coreplatform.kafka.event

import com.asyncsite.coreplatform.kafka.event.examples.UserRegisteredEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class EventEnvelopeTest {
    
    @Test
    fun `should create envelope with event and metadata`() {
        // Given
        val event = UserRegisteredEvent(
            userId = "user-123",
            email = "test@example.com",
            name = "Test User"
        )
        val metadata = EventMetadata(
            correlationId = "correlation-123",
            userId = "user-123"
        )
        val topic = "asyncsite.user.events"
        
        // When
        val envelope = EventEnvelope(
            event = event,
            metadata = metadata,
            topic = topic
        )
        
        // Then
        assertThat(envelope.event).isEqualTo(event)
        assertThat(envelope.metadata).isEqualTo(metadata)
        assertThat(envelope.topic).isEqualTo(topic)
        assertThat(envelope.eventId).isEqualTo(event.eventId)
        assertThat(envelope.eventType).isEqualTo("UserRegistered")
        assertThat(envelope.createdAt).isBeforeOrEqualTo(Instant.now())
    }
    
    @Test
    fun `should create envelope with default metadata`() {
        // Given
        val event = UserRegisteredEvent(
            userId = "user-123",
            email = "test@example.com",
            name = "Test User"
        )
        
        // When
        val envelope = EventEnvelope(event = event)
        
        // Then
        assertThat(envelope.event).isEqualTo(event)
        assertThat(envelope.metadata).isEqualTo(EventMetadata())
        assertThat(envelope.topic).isNull()
    }
    
    @Test
    fun `should create envelope using companion object methods`() {
        // Given
        val event = UserRegisteredEvent(
            userId = "user-123",
            email = "test@example.com",
            name = "Test User"
        )
        
        // When - Using of(event)
        val envelope1 = EventEnvelope.of(event)
        
        // Then
        assertThat(envelope1.event).isEqualTo(event)
        assertThat(envelope1.metadata).isEqualTo(EventMetadata())
        
        // When - Using of(event, correlationId)
        val envelope2 = EventEnvelope.of(event, "correlation-123")
        
        // Then
        assertThat(envelope2.event).isEqualTo(event)
        assertThat(envelope2.metadata.correlationId).isEqualTo("correlation-123")
        
        // When - Using of(event, metadata)
        val metadata = EventMetadata(userId = "user-123")
        val envelope3 = EventEnvelope.of(event, metadata)
        
        // Then
        assertThat(envelope3.event).isEqualTo(event)
        assertThat(envelope3.metadata).isEqualTo(metadata)
    }
    
    @Test
    fun `should update metadata with withMetadata method`() {
        // Given
        val event = UserRegisteredEvent(
            userId = "user-123",
            email = "test@example.com",
            name = "Test User"
        )
        val originalMetadata = EventMetadata(correlationId = "original")
        val envelope = EventEnvelope(event = event, metadata = originalMetadata)
        
        val newMetadata = EventMetadata(
            correlationId = "new-correlation",
            userId = "user-123"
        )
        
        // When
        val updatedEnvelope = envelope.withMetadata(newMetadata)
        
        // Then
        assertThat(updatedEnvelope.metadata).isEqualTo(newMetadata)
        assertThat(updatedEnvelope.event).isEqualTo(event)
        // Original envelope should not be modified
        assertThat(envelope.metadata).isEqualTo(originalMetadata)
    }
    
    @Test
    fun `should add headers with withHeaders method`() {
        // Given
        val event = UserRegisteredEvent(
            userId = "user-123",
            email = "test@example.com",
            name = "Test User"
        )
        val envelope = EventEnvelope(event = event)
        
        val headers = mapOf(
            "header1" to "value1",
            "header2" to "value2"
        )
        
        // When
        val updatedEnvelope = envelope.withHeaders(headers)
        
        // Then
        assertThat(updatedEnvelope.metadata.headers).containsAllEntriesOf(headers)
        assertThat(envelope.metadata.headers).isEmpty()
    }
    
    @Test
    fun `should update correlation ID with withCorrelationId method`() {
        // Given
        val event = UserRegisteredEvent(
            userId = "user-123",
            email = "test@example.com",
            name = "Test User"
        )
        val envelope = EventEnvelope(event = event)
        
        // When
        val updatedEnvelope = envelope.withCorrelationId("new-correlation-123")
        
        // Then
        assertThat(updatedEnvelope.metadata.correlationId).isEqualTo("new-correlation-123")
        assertThat(envelope.metadata.correlationId).isNull()
    }
    
    @Test
    fun `should update topic with withTopic method`() {
        // Given
        val event = UserRegisteredEvent(
            userId = "user-123",
            email = "test@example.com",
            name = "Test User"
        )
        val envelope = EventEnvelope(event = event)
        
        // When
        val updatedEnvelope = envelope.withTopic("asyncsite.user.events")
        
        // Then
        assertThat(updatedEnvelope.topic).isEqualTo("asyncsite.user.events")
        assertThat(envelope.topic).isNull()
    }
    
    @Test
    fun `should chain multiple updates fluently`() {
        // Given
        val event = UserRegisteredEvent(
            userId = "user-123",
            email = "test@example.com",
            name = "Test User"
        )
        
        // When
        val envelope = EventEnvelope.of(event)
            .withCorrelationId("correlation-123")
            .withHeaders(mapOf("source" to "test"))
            .withTopic("asyncsite.user.events")
        
        // Then
        assertThat(envelope.metadata.correlationId).isEqualTo("correlation-123")
        assertThat(envelope.metadata.headers).containsEntry("source", "test")
        assertThat(envelope.topic).isEqualTo("asyncsite.user.events")
        assertThat(envelope.event).isEqualTo(event)
    }
}
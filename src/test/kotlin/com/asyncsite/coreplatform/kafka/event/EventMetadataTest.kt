package com.asyncsite.coreplatform.kafka.event

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class EventMetadataTest {
    
    @Test
    fun `should create metadata with all fields`() {
        // Given
        val correlationId = "correlation-123"
        val causationId = "causation-456"
        val userId = "user-789"
        val source = "test-service"
        val headers = mapOf("custom-header" to "value")
        val publishedAt = Instant.now()
        val partitionKey = "partition-1"
        
        // When
        val metadata = EventMetadata(
            correlationId = correlationId,
            causationId = causationId,
            userId = userId,
            source = source,
            headers = headers,
            publishedAt = publishedAt,
            partitionKey = partitionKey
        )
        
        // Then
        assertThat(metadata.correlationId).isEqualTo(correlationId)
        assertThat(metadata.causationId).isEqualTo(causationId)
        assertThat(metadata.userId).isEqualTo(userId)
        assertThat(metadata.source).isEqualTo(source)
        assertThat(metadata.headers).isEqualTo(headers)
        assertThat(metadata.publishedAt).isEqualTo(publishedAt)
        assertThat(metadata.partitionKey).isEqualTo(partitionKey)
    }
    
    @Test
    fun `should create metadata from headers`() {
        // Given
        val headers = mapOf(
            EventMetadata.CORRELATION_ID_HEADER to "correlation-123",
            EventMetadata.CAUSATION_ID_HEADER to "causation-456",
            EventMetadata.USER_ID_HEADER to "user-789",
            EventMetadata.SOURCE_HEADER to "test-service",
            "custom-header" to "custom-value"
        )
        
        // When
        val metadata = EventMetadata.fromHeaders(headers)
        
        // Then
        assertThat(metadata.correlationId).isEqualTo("correlation-123")
        assertThat(metadata.causationId).isEqualTo("causation-456")
        assertThat(metadata.userId).isEqualTo("user-789")
        assertThat(metadata.source).isEqualTo("test-service")
        assertThat(metadata.headers).containsAllEntriesOf(headers)
    }
    
    @Test
    fun `should convert metadata to headers`() {
        // Given
        val publishedAt = Instant.now()
        val metadata = EventMetadata(
            correlationId = "correlation-123",
            causationId = "causation-456",
            userId = "user-789",
            source = "test-service",
            publishedAt = publishedAt,
            headers = mapOf("custom" to "value")
        )
        
        // When
        val headers = metadata.toHeaders()
        
        // Then
        assertThat(headers).containsEntry(EventMetadata.CORRELATION_ID_HEADER, "correlation-123")
        assertThat(headers).containsEntry(EventMetadata.CAUSATION_ID_HEADER, "causation-456")
        assertThat(headers).containsEntry(EventMetadata.USER_ID_HEADER, "user-789")
        assertThat(headers).containsEntry(EventMetadata.SOURCE_HEADER, "test-service")
        assertThat(headers).containsEntry(EventMetadata.PUBLISHED_AT_HEADER, publishedAt.toString())
        assertThat(headers).containsEntry("custom", "value")
    }
    
    @Test
    fun `should handle null values in toHeaders`() {
        // Given
        val metadata = EventMetadata()
        
        // When
        val headers = metadata.toHeaders()
        
        // Then
        assertThat(headers).isEmpty()
    }
    
    @Test
    fun `should add headers with withHeaders method`() {
        // Given
        val metadata = EventMetadata(
            correlationId = "correlation-123",
            headers = mapOf("existing" to "value")
        )
        val additionalHeaders = mapOf(
            "new-header" to "new-value",
            "another" to "another-value"
        )
        
        // When
        val updatedMetadata = metadata.withHeaders(additionalHeaders)
        
        // Then
        assertThat(updatedMetadata.headers).hasSize(3)
        assertThat(updatedMetadata.headers).containsEntry("existing", "value")
        assertThat(updatedMetadata.headers).containsEntry("new-header", "new-value")
        assertThat(updatedMetadata.headers).containsEntry("another", "another-value")
        assertThat(updatedMetadata.correlationId).isEqualTo("correlation-123")
    }
    
    @Test
    fun `should update correlation ID with withCorrelationId method`() {
        // Given
        val metadata = EventMetadata(
            correlationId = "old-correlation",
            userId = "user-123"
        )
        
        // When
        val updatedMetadata = metadata.withCorrelationId("new-correlation")
        
        // Then
        assertThat(updatedMetadata.correlationId).isEqualTo("new-correlation")
        assertThat(updatedMetadata.userId).isEqualTo("user-123")
        // Original should not be modified
        assertThat(metadata.correlationId).isEqualTo("old-correlation")
    }
    
    @Test
    fun `should preserve all fields when copying`() {
        // Given
        val original = EventMetadata(
            correlationId = "correlation-123",
            causationId = "causation-456",
            userId = "user-789",
            source = "test-service",
            headers = mapOf("header" to "value"),
            publishedAt = Instant.now(),
            partitionKey = "partition-1"
        )
        
        // When
        val copy = original.copy()
        
        // Then
        assertThat(copy).isEqualTo(original)
        assertThat(copy).isNotSameAs(original)
    }
}
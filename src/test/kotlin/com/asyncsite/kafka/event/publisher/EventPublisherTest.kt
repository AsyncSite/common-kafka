package com.asyncsite.kafka.event.publisher

import com.asyncsite.kafka.event.examples.UserRegisteredEvent
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.slf4j.MDC
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import java.util.concurrent.CompletableFuture

class EventPublisherTest {
    
    private lateinit var kafkaTemplate: KafkaTemplate<String, Any>
    private lateinit var eventPublisher: EventPublisher
    private lateinit var sendResult: SendResult<String, Any>
    private lateinit var future: CompletableFuture<SendResult<String, Any>>
    
    @BeforeEach
    fun setup() {
        kafkaTemplate = mockk()
        eventPublisher = EventPublisher(kafkaTemplate)
        sendResult = mockk()
        future = CompletableFuture.completedFuture(sendResult)
        
        MDC.clear()
    }
    
    @AfterEach
    fun tearDown() {
        MDC.clear()
        clearAllMocks()
    }
    
    @Test
    fun `should publish event with generated correlation ID when not in MDC`() {
        // Given
        val event = UserRegisteredEvent(
            userId = "user123",
            email = "test@example.com",
            name = "Test User"
        )
        val topic = "asyncsite.user.events"
        
        every { kafkaTemplate.send(any<org.apache.kafka.clients.producer.ProducerRecord<String, Any>>()) } returns future
        
        // When
        val result = eventPublisher.publishAsync(event, topic)
        
        // Then
        assertThat(result).isEqualTo(future)
        verify(exactly = 1) { 
            kafkaTemplate.send(any<org.apache.kafka.clients.producer.ProducerRecord<String, Any>>()) 
        }
    }
    
    @Test
    fun `should publish event with correlation ID from MDC`() {
        // Given
        val correlationId = "test-correlation-123"
        MDC.put("correlationId", correlationId)
        
        val event = UserRegisteredEvent(
            userId = "user123",
            email = "test@example.com",
            name = "Test User"
        )
        val topic = "asyncsite.user.events"
        
        every { kafkaTemplate.send(any<org.apache.kafka.clients.producer.ProducerRecord<String, Any>>()) } returns future
        
        // When
        val result = eventPublisher.publishAsync(event, topic)
        
        // Then
        assertThat(result).isEqualTo(future)
        verify(exactly = 1) { 
            kafkaTemplate.send(any<org.apache.kafka.clients.producer.ProducerRecord<String, Any>>()) 
        }
    }
    
    @Test
    fun `should publish event with specific partition key`() {
        // Given
        val event = UserRegisteredEvent(
            userId = "user123",
            email = "test@example.com",
            name = "Test User"
        )
        val topic = "asyncsite.user.events"
        val partitionKey = "user123"
        
        every { kafkaTemplate.send(any<org.apache.kafka.clients.producer.ProducerRecord<String, Any>>()) } returns future
        
        // When
        val result = eventPublisher.publishAsyncWithKey(event, topic, partitionKey)
        
        // Then
        assertThat(result).isEqualTo(future)
        verify(exactly = 1) { 
            kafkaTemplate.send(any<org.apache.kafka.clients.producer.ProducerRecord<String, Any>>()) 
        }
    }
    
    @Test
    fun `should handle publish failure`() {
        // Given
        val event = UserRegisteredEvent(
            userId = "user123",
            email = "test@example.com",
            name = "Test User"
        )
        val topic = "asyncsite.user.events"
        val exception = RuntimeException("Kafka unavailable")
        
        val failedFuture = CompletableFuture<SendResult<String, Any>>()
        failedFuture.completeExceptionally(exception)
        
        every { kafkaTemplate.send(any<org.apache.kafka.clients.producer.ProducerRecord<String, Any>>()) } returns failedFuture
        
        // When
        val result = eventPublisher.publishAsync(event, topic)
        
        // Then
        assertThat(result.isCompletedExceptionally).isTrue
        verify(exactly = 1) { 
            kafkaTemplate.send(any<org.apache.kafka.clients.producer.ProducerRecord<String, Any>>()) 
        }
    }
    
    @Test
    fun `should publish batch of events`() {
        // Given
        val events = listOf(
            UserRegisteredEvent("user1", "test1@example.com", "User 1"),
            UserRegisteredEvent("user2", "test2@example.com", "User 2"),
            UserRegisteredEvent("user3", "test3@example.com", "User 3")
        )
        val topic = "asyncsite.user.events"
        
        every { kafkaTemplate.send(any<org.apache.kafka.clients.producer.ProducerRecord<String, Any>>()) } returns future
        
        // When
        val results = eventPublisher.publishBatch(events, topic)
        
        // Then
        assertThat(results).hasSize(3)
        verify(exactly = 3) { 
            kafkaTemplate.send(any<org.apache.kafka.clients.producer.ProducerRecord<String, Any>>()) 
        }
    }
    
    @Test
    fun `should publish synchronously with timeout`() {
        // Given
        val event = UserRegisteredEvent(
            userId = "user123",
            email = "test@example.com",
            name = "Test User"
        )
        val topic = "asyncsite.user.events"
        
        every { kafkaTemplate.send(any<org.apache.kafka.clients.producer.ProducerRecord<String, Any>>()) } returns future
        
        // When
        val result = eventPublisher.publishSync(event, topic, 1000)
        
        // Then
        assertThat(result).isEqualTo(sendResult)
        verify(exactly = 1) { 
            kafkaTemplate.send(any<org.apache.kafka.clients.producer.ProducerRecord<String, Any>>()) 
        }
    }
    
    @Test
    fun `should throw exception when sync publish times out`() {
        // Given
        val event = UserRegisteredEvent(
            userId = "user123",
            email = "test@example.com",
            name = "Test User"
        )
        val topic = "asyncsite.user.events"
        
        val neverCompleteFuture = CompletableFuture<SendResult<String, Any>>()
        every { kafkaTemplate.send(any<org.apache.kafka.clients.producer.ProducerRecord<String, Any>>()) } returns neverCompleteFuture
        
        // When/Then
        assertThrows<Exception> {
            eventPublisher.publishSync(event, topic, 100)
        }
    }
}
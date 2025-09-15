package com.asyncsite.coreplatform.kafka.config

import com.asyncsite.coreplatform.kafka.correlation.KafkaCorrelationIdConsumerInterceptor
import mu.KotlinLogging
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.RecordInterceptor
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
import org.springframework.kafka.support.serializer.JsonDeserializer

private val log = KotlinLogging.logger {}

/**
 * Kafka consumer configuration for the AsyncSite platform.
 * Configures consumer with correlation ID handling and error recovery.
 */
@Configuration
@EnableKafka
class KafkaConsumerConfig {
    
    @Value("\${spring.kafka.bootstrap-servers:localhost:9092}")
    private lateinit var bootstrapServers: String
    
    @Value("\${spring.application.name:default-service}")
    private lateinit var applicationName: String
    
    @Value("\${spring.kafka.consumer.auto-offset-reset:earliest}")
    private lateinit var autoOffsetReset: String
    
    @Value("\${spring.kafka.consumer.enable-auto-commit:false}")
    private var enableAutoCommit: Boolean = false
    
    @Value("\${spring.kafka.consumer.max-poll-records:500}")
    private var maxPollRecords: Int = 500
    
    @Value("\${spring.kafka.consumer.session-timeout-ms:30000}")
    private var sessionTimeoutMs: Int = 30000
    
    @Value("\${spring.kafka.consumer.heartbeat-interval-ms:3000}")
    private var heartbeatIntervalMs: Int = 3000
    
    /**
     * Creates the consumer factory with all configurations.
     */
    @Bean
    fun consumerFactory(): ConsumerFactory<String, Any> {
        val configs = mutableMapOf<String, Any>()
        
        // Connection settings
        configs[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
        configs[ConsumerConfig.GROUP_ID_CONFIG] = applicationName
        
        // Deserialization with error handling
        configs[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = 
            ErrorHandlingDeserializer::class.java
        configs[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = 
            ErrorHandlingDeserializer::class.java
        
        // Delegate deserializers
        configs[ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS] = 
            StringDeserializer::class.java
        configs[ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS] = 
            JsonDeserializer::class.java
        
        // Consumer behavior
        configs[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = autoOffsetReset
        configs[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = enableAutoCommit
        configs[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = maxPollRecords
        
        // Session management
        configs[ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG] = sessionTimeoutMs
        configs[ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG] = heartbeatIntervalMs
        
        // Interceptor for correlation ID
        configs[ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG] = 
            KafkaCorrelationIdConsumerInterceptor::class.java.name
        
        // JSON deserializer settings
        configs[JsonDeserializer.TRUSTED_PACKAGES] = "com.asyncsite.*"
        configs[JsonDeserializer.REMOVE_TYPE_INFO_HEADERS] = false
        configs[JsonDeserializer.USE_TYPE_INFO_HEADERS] = false  // Disable type headers for microservices independence
        configs[JsonDeserializer.VALUE_DEFAULT_TYPE] = "com.fasterxml.jackson.databind.JsonNode"  // Default to JsonNode for flexibility
        
        log.info { 
            "Configuring Kafka consumer - Bootstrap servers: $bootstrapServers, " +
            "Group ID: $applicationName, Auto offset reset: $autoOffsetReset" 
        }
        
        return DefaultKafkaConsumerFactory(configs)
    }
    
    /**
     * Creates the Kafka listener container factory for @KafkaListener.
     */
    @Bean
    fun kafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, Any> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, Any>()
        factory.consumerFactory = consumerFactory()
        
        // Container properties
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
        factory.containerProperties.isSyncCommits = true
        
        // Error handling
        factory.setCommonErrorHandler(kafkaErrorHandler())
        
        // Concurrency settings (can be overridden per listener)
        factory.setConcurrency(3)
        
        // Record interceptor for correlation ID extraction
        factory.setRecordInterceptor(correlationIdRecordInterceptor())
        
        // Enable batch processing if needed
        // factory.isBatchListener = true
        
        // Enable observation for metrics
        factory.containerProperties.isObservationEnabled = true
        
        return factory
    }
    
    /**
     * Creates a record interceptor that extracts correlation ID from headers.
     */
    @Bean
    fun correlationIdRecordInterceptor(): RecordInterceptor<String, Any> {
        return RecordInterceptor { record, consumer ->
            // Extract correlation ID from header
            val correlationIdHeader = record.headers().lastHeader("X-Correlation-Id")
            if (correlationIdHeader != null) {
                val correlationId = String(correlationIdHeader.value())
                MDC.put("correlationId", correlationId)
                
                log.trace { 
                    "Set correlation ID from record: $correlationId, " +
                    "Topic: ${record.topic()}, Partition: ${record.partition()}, " +
                    "Offset: ${record.offset()}" 
                }
            }
            
            // Extract other useful headers
            record.headers().lastHeader("X-User-Id")?.let {
                MDC.put("userId", String(it.value()))
            }
            
            record.headers().lastHeader("X-Causation-Id")?.let {
                MDC.put("causationId", String(it.value()))
            }
            
            record
        }
    }
    
    /**
     * Creates an error handler for consumer errors.
     */
    @Bean
    fun kafkaErrorHandler(): org.springframework.kafka.listener.DefaultErrorHandler {
        val errorHandler = org.springframework.kafka.listener.DefaultErrorHandler()
        
        // Configure retry behavior
        errorHandler.setBackOffFunction { record, exception ->
            log.warn { 
                "Error processing record from topic ${record.topic()}, " +
                "partition ${record.partition()}, offset ${record.offset()}: " +
                "${exception.message}" 
            }
            
            // Fixed backoff: retry 3 times with 1 second delay
            org.springframework.util.backoff.FixedBackOff(1000L, 3)
        }
        
        return errorHandler
    }
    
    /**
     * Creates a factory for batch processing if needed.
     * Uncomment to enable batch consumption.
     */
    // @Bean
    // fun batchKafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, Any> {
    //     val factory = ConcurrentKafkaListenerContainerFactory<String, Any>()
    //     factory.consumerFactory = consumerFactory()
    //     factory.isBatchListener = true
    //     factory.containerProperties.ackMode = ContainerProperties.AckMode.BATCH
    //     return factory
    // }
}
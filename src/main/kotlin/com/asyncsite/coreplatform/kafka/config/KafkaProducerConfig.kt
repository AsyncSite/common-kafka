package com.asyncsite.coreplatform.kafka.config

import com.asyncsite.coreplatform.kafka.correlation.KafkaCorrelationIdProducerInterceptor
import mu.KotlinLogging
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.support.serializer.JsonSerializer

private val log = KotlinLogging.logger {}

/**
 * Kafka producer configuration for the AsyncSite platform.
 * Configures producer with correlation ID interceptor and optimized settings.
 */
@Configuration
class KafkaProducerConfig {
    
    @Value("\${spring.kafka.bootstrap-servers:localhost:9092}")
    private lateinit var bootstrapServers: String
    
    @Value("\${spring.kafka.producer.acks:all}")
    private lateinit var acks: String
    
    @Value("\${spring.kafka.producer.retries:3}")
    private var retries: Int = 3
    
    @Value("\${spring.kafka.producer.batch-size:16384}")
    private var batchSize: Int = 16384
    
    @Value("\${spring.kafka.producer.linger-ms:10}")
    private var lingerMs: Int = 10
    
    @Value("\${spring.kafka.producer.buffer-memory:33554432}")
    private var bufferMemory: Int = 33554432
    
    @Value("\${spring.kafka.producer.compression-type:snappy}")
    private lateinit var compressionType: String
    
    @Value("\${spring.kafka.producer.enable-idempotence:false}")
    private var enableIdempotence: Boolean = false
    
    /**
     * Creates the producer factory with all configurations.
     */
    @Bean
    fun producerFactory(): ProducerFactory<String, Any> {
        val configs = mutableMapOf<String, Any>()
        
        // Connection settings
        configs[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
        
        // Serialization
        configs[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        configs[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = JsonSerializer::class.java
        
        // Reliability settings
        configs[ProducerConfig.ACKS_CONFIG] = acks
        configs[ProducerConfig.RETRIES_CONFIG] = retries
        configs[ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG] = enableIdempotence
        
        // Performance settings
        configs[ProducerConfig.BATCH_SIZE_CONFIG] = batchSize
        configs[ProducerConfig.LINGER_MS_CONFIG] = lingerMs
        configs[ProducerConfig.BUFFER_MEMORY_CONFIG] = bufferMemory
        configs[ProducerConfig.COMPRESSION_TYPE_CONFIG] = compressionType
        
        // Interceptor for correlation ID
        configs[ProducerConfig.INTERCEPTOR_CLASSES_CONFIG] = 
            KafkaCorrelationIdProducerInterceptor::class.java.name
        
        // Timeout settings
        configs[ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG] = 30000
        configs[ProducerConfig.MAX_BLOCK_MS_CONFIG] = 60000
        
        // JSON serializer settings
        configs[JsonSerializer.ADD_TYPE_INFO_HEADERS] = false
        configs[JsonSerializer.TYPE_MAPPINGS] = 
            "event:com.asyncsite.coreplatform.kafka.event.BaseEvent"
        
        log.info { 
            "Configuring Kafka producer - Bootstrap servers: $bootstrapServers, " +
            "Acks: $acks, Compression: $compressionType, Idempotence: $enableIdempotence" 
        }
        
        return DefaultKafkaProducerFactory(configs)
    }
    
    /**
     * Creates the KafkaTemplate for sending messages.
     */
    @Bean
    fun kafkaTemplate(): KafkaTemplate<String, Any> {
        val template = KafkaTemplate(producerFactory())
        
        // Warm up the producer to avoid blocking on first send
        // This forces KafkaProducer initialization at startup
        try {
            template.setAllowNonTransactional(true)
            // Force producer initialization by getting metrics
            template.metrics()
            log.info { "Kafka producer warmed up successfully" }
        } catch (e: Exception) {
            log.warn { "Failed to warm up Kafka producer: ${e.message}" }
        }
        
        // Set default topic if configured
        // template.defaultTopic = "default-topic"
        
        // Enable observation for metrics
        template.setObservationEnabled(true)
        
        return template
    }
    
    /**
     * Creates a transactional KafkaTemplate if needed.
     * Uncomment and configure transaction ID prefix to enable.
     */
    // @Bean
    // fun transactionalKafkaTemplate(): KafkaTemplate<String, Any> {
    //     val factory = producerFactory()
    //     factory.setTransactionIdPrefix("tx-")
    //     return KafkaTemplate(factory)
    // }
}
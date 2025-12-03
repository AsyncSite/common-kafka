package com.asyncsite.kafka.config

import mu.KotlinLogging
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.kafka.core.KafkaTemplate

private val log = KotlinLogging.logger {}

/**
 * Auto-configuration for AsyncSite Kafka module.
 * Automatically configures Kafka components when included as a dependency.
 */
@AutoConfiguration
@ConditionalOnClass(KafkaTemplate::class)
@ConditionalOnProperty(
    prefix = "asyncsite.kafka",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
@ComponentScan(basePackages = [
    "com.asyncsite.coreplatform.kafka.event",
    "com.asyncsite.coreplatform.kafka.correlation"
])
@Import(
    KafkaProducerConfig::class,
    KafkaConsumerConfig::class
)
class KafkaAutoConfiguration {

    init {
        log.info { "AsyncSite Kafka auto-configuration initialized" }
    }
}
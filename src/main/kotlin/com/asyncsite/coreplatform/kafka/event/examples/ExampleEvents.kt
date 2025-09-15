package com.asyncsite.coreplatform.kafka.event.examples

import com.asyncsite.coreplatform.kafka.event.*
import java.math.BigDecimal
import java.time.Instant

/**
 * Example: Simple domain event
 */
data class UserRegisteredEvent(
    val userId: String,
    val email: String,
    val name: String,
    val registeredFrom: String = "web"
) : BaseEvent() {
    override val eventType = "UserRegistered"
}

/**
 * Example: Event with user context
 */
data class ProfileUpdatedEvent(
    val profileId: String,
    val updatedFields: Set<String>,
    override val userId: String?,
    override val userEmail: String?,
    override val userRoles: Set<String>?
) : BaseEvent(), UserContextEvent {
    override val eventType = "ProfileUpdated"
}

/**
 * Example: Event with retry capability
 */
data class EmailSendRequestedEvent(
    val emailId: String,
    val to: String,
    val subject: String,
    val template: String,
    val variables: Map<String, Any> = emptyMap(),
    override val maxRetries: Int = 5,
    override val retryDelayMs: Long = 2000
) : BaseEvent(), RetryableEvent {
    override val eventType = "EmailSendRequested"
}

/**
 * Example: Aggregate event for Event Sourcing
 */
data class StudyCreatedEvent(
    override val aggregateId: String,
    val title: String,
    val description: String,
    val leaderId: String,
    val maxMembers: Int,
    val category: String
) : BaseEvent(), AggregateEvent {
    override val eventType = "StudyCreated"
    override val aggregateType = "Study"
}

/**
 * Example: Compensatable event for Saga pattern
 */
data class PaymentProcessedEvent(
    val paymentId: String,
    val orderId: String,
    val amount: BigDecimal,
    val currency: String,
    override val compensatingEventType: String = "PaymentReversed"
) : BaseEvent(), CompensatableEvent {
    override val eventType = "PaymentProcessed"
}

/**
 * Example: Complex event with multiple interfaces
 */
data class OrderPlacedEvent(
    override val aggregateId: String,
    val customerId: String,
    val items: List<OrderItem>,
    val totalAmount: BigDecimal,
    override val userId: String?,
    override val userEmail: String?,
    override val userRoles: Set<String>?,
    override val maxRetries: Int = 3
) : BaseEvent(), AggregateEvent, UserContextEvent, RetryableEvent {
    override val eventType = "OrderPlaced"
    override val aggregateType = "Order"
    
    data class OrderItem(
        val productId: String,
        val quantity: Int,
        val price: BigDecimal
    )
}

/**
 * Example: Notification event
 */
data class NotificationRequestedEvent(
    val notificationId: String,
    val userId: String,
    val type: NotificationType,
    val channels: Set<NotificationChannel>,
    val title: String,
    val message: String,
    val data: Map<String, Any> = emptyMap()
) : BaseEvent() {
    override val eventType = "NotificationRequested"
    
    enum class NotificationType {
        INFO, WARNING, ERROR, SUCCESS,
        STUDY_APPLICATION, STUDY_ACCEPTED, STUDY_REJECTED,
        PAYMENT_SUCCESS, PAYMENT_FAILED
    }
    
    enum class NotificationChannel {
        IN_APP, PUSH, EMAIL, SMS, WEBSOCKET
    }
}
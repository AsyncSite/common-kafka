package com.asyncsite.kafka.event;

import java.io.Serializable;

/**
 * Service lifecycle event DTO for startup/shutdown notifications via Kafka.
 * Published by services on ApplicationReadyEvent (STARTED) and ContextClosedEvent (STOPPED).
 * Consumed by noti-service ServiceLifecycleEventListener to send Discord #infra alerts.
 *
 * <p>This is a plain Java POJO (no Lombok, no Kotlin dependencies)
 * so it can be used by any service regardless of their dependency setup.
 * Services that do not depend on common-kafka can define their own
 * identical DTO and publish to the same topic.</p>
 */
public class ServiceLifecycleEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String TOPIC = "asyncsite.service.lifecycle";

    private String serviceName;
    private String eventType; // STARTED or STOPPED
    private String hostname;
    private String timestamp;
    private String javaVersion;
    private String springProfiles;
    private Long startupTimeMs; // only for STARTED events

    public ServiceLifecycleEvent() {
    }

    private ServiceLifecycleEvent(Builder builder) {
        this.serviceName = builder.serviceName;
        this.eventType = builder.eventType;
        this.hostname = builder.hostname;
        this.timestamp = builder.timestamp;
        this.javaVersion = builder.javaVersion;
        this.springProfiles = builder.springProfiles;
        this.startupTimeMs = builder.startupTimeMs;
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters

    public String getServiceName() {
        return serviceName;
    }

    public String getEventType() {
        return eventType;
    }

    public String getHostname() {
        return hostname;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getJavaVersion() {
        return javaVersion;
    }

    public String getSpringProfiles() {
        return springProfiles;
    }

    public Long getStartupTimeMs() {
        return startupTimeMs;
    }

    // Setters (needed for Jackson deserialization)

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public void setJavaVersion(String javaVersion) {
        this.javaVersion = javaVersion;
    }

    public void setSpringProfiles(String springProfiles) {
        this.springProfiles = springProfiles;
    }

    public void setStartupTimeMs(Long startupTimeMs) {
        this.startupTimeMs = startupTimeMs;
    }

    @Override
    public String toString() {
        return "ServiceLifecycleEvent{" +
                "serviceName='" + serviceName + '\'' +
                ", eventType='" + eventType + '\'' +
                ", hostname='" + hostname + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", springProfiles='" + springProfiles + '\'' +
                ", startupTimeMs=" + startupTimeMs +
                '}';
    }

    public static class Builder {
        private String serviceName;
        private String eventType;
        private String hostname;
        private String timestamp;
        private String javaVersion;
        private String springProfiles;
        private Long startupTimeMs;

        public Builder serviceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public Builder eventType(String eventType) {
            this.eventType = eventType;
            return this;
        }

        public Builder hostname(String hostname) {
            this.hostname = hostname;
            return this;
        }

        public Builder timestamp(String timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder javaVersion(String javaVersion) {
            this.javaVersion = javaVersion;
            return this;
        }

        public Builder springProfiles(String springProfiles) {
            this.springProfiles = springProfiles;
            return this;
        }

        public Builder startupTimeMs(Long startupTimeMs) {
            this.startupTimeMs = startupTimeMs;
            return this;
        }

        public ServiceLifecycleEvent build() {
            return new ServiceLifecycleEvent(this);
        }
    }
}

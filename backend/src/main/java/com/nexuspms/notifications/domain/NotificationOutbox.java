package com.nexuspms.notifications.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * HLD S8.1 outbox pattern: written in the SAME transaction as the triggering
 * domain event's handler (NotificationDispatchService's @EventListener runs
 * synchronously in the publisher's transaction by default), then dispatched
 * asynchronously by NotificationDispatchJobHandler via the shared
 * BackgroundJobWorker (common/job).
 */
@Entity
@Table(name = "notification_outbox")
public class NotificationOutbox {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "recipient_id", nullable = false)
    private UUID recipientId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, Object> payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status = OutboxStatus.PENDING;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected NotificationOutbox() {
        // JPA
    }

    public NotificationOutbox(UUID recipientId, String eventType, Map<String, Object> payload, NotificationChannel channel) {
        this.recipientId = recipientId;
        this.eventType = eventType;
        this.payload = payload;
        this.channel = channel;
        this.nextAttemptAt = Instant.now();
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public void markSent() {
        this.status = OutboxStatus.SENT;
    }

    public void markFailed(Instant nextAttemptAt, boolean deadLetter) {
        this.attemptCount++;
        this.status = deadLetter ? OutboxStatus.DEAD_LETTERED : OutboxStatus.FAILED;
        this.nextAttemptAt = nextAttemptAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getRecipientId() {
        return recipientId;
    }

    public String getEventType() {
        return eventType;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public OutboxStatus getStatus() {
        return status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }
}

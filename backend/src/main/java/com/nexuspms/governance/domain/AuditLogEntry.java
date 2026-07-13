package com.nexuspms.governance.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** PRD FR-15..17: append-only, written synchronously in the same transaction as the governance action it records (LLD S4.4). */
@Entity
@Table(name = "audit_log")
public class AuditLogEntry {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "actor_id", nullable = false)
    private UUID actorId;

    @Column(name = "action_type", nullable = false)
    private String actionType;

    @Column(name = "target_entity_type", nullable = false)
    private String targetEntityType;

    @Column(name = "target_entity_id", nullable = false)
    private UUID targetEntityId;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AuditLogEntry() {
        // JPA
    }

    public AuditLogEntry(UUID actorId, String actionType, String targetEntityType, UUID targetEntityId, Map<String, Object> metadata) {
        this.actorId = actorId;
        this.actionType = actionType;
        this.targetEntityType = targetEntityType;
        this.targetEntityId = targetEntityId;
        this.metadata = metadata;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getActorId() {
        return actorId;
    }

    public String getActionType() {
        return actionType;
    }

    public String getTargetEntityType() {
        return targetEntityType;
    }

    public UUID getTargetEntityId() {
        return targetEntityId;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

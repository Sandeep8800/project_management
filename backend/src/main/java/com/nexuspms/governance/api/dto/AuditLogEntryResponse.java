package com.nexuspms.governance.api.dto;

import com.nexuspms.governance.domain.AuditLogEntry;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditLogEntryResponse(
        UUID id, UUID actorId, String actionType, String targetEntityType,
        UUID targetEntityId, Map<String, Object> metadata, Instant createdAt
) {
    public static AuditLogEntryResponse from(AuditLogEntry e) {
        return new AuditLogEntryResponse(
                e.getId(), e.getActorId(), e.getActionType(), e.getTargetEntityType(),
                e.getTargetEntityId(), e.getMetadata(), e.getCreatedAt());
    }
}

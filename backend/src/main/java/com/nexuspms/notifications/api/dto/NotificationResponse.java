package com.nexuspms.notifications.api.dto;

import com.nexuspms.notifications.domain.Notification;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record NotificationResponse(UUID id, String eventType, Map<String, Object> payload, Instant readAt, Instant createdAt) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.getId(), n.getEventType(), n.getPayload(), n.getReadAt(), n.getCreatedAt());
    }
}

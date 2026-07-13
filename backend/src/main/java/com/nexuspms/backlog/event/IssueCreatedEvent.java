package com.nexuspms.backlog.event;

import java.util.UUID;

/** LLD S9 domain event catalog. Consumed by Reporting, Notifications, Real-Time push, Sprint & Board (board state). */
public record IssueCreatedEvent(UUID issueId, UUID projectId) {
}

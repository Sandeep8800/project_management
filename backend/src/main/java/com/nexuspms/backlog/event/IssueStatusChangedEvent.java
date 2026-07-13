package com.nexuspms.backlog.event;

import java.util.UUID;

/** LLD S9/S11.1: the central event driving Reporting projections, Notifications, and Real-Time board push. */
public record IssueStatusChangedEvent(UUID issueId, UUID projectId, UUID sprintId, String fromStatus, String toStatus) {
}

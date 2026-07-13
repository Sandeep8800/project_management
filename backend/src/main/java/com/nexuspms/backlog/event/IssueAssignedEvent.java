package com.nexuspms.backlog.event;

import java.util.UUID;

public record IssueAssignedEvent(UUID issueId, UUID projectId, UUID assigneeId) {
}

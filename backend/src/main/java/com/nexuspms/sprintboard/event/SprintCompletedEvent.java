package com.nexuspms.sprintboard.event;

import java.util.UUID;

/** LLD S11.2: triggers Reporting's SprintSummaryService and Notifications; large rollovers are offloaded to a background job (see RolloverDecision). */
public record SprintCompletedEvent(UUID sprintId, UUID projectId, int carryOverIssueCount) {
}

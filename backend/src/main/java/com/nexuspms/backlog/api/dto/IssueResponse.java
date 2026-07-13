package com.nexuspms.backlog.api.dto;

import com.nexuspms.backlog.domain.Issue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record IssueResponse(
        UUID id,
        UUID projectId,
        String issueKey,
        String issueType,
        UUID parentIssueId,
        String title,
        String description,
        String status,
        UUID assigneeId,
        UUID reporterId,
        String priority,
        BigDecimal storyPoints,
        UUID sprintId,
        String backlogRank,
        long version,
        Instant createdAt,
        Instant updatedAt
) {
    public static IssueResponse from(Issue issue) {
        return new IssueResponse(
                issue.getId(), issue.getProjectId(), issue.getIssueKey(), issue.getIssueType().name(),
                issue.getParentIssueId(), issue.getTitle(), issue.getDescription(), issue.getStatus(),
                issue.getAssigneeId(), issue.getReporterId(), issue.getPriority(), issue.getStoryPoints(),
                issue.getSprintId(), issue.getBacklogRank().toPlainString(), issue.getVersion(),
                issue.getCreatedAt(), issue.getUpdatedAt());
    }
}

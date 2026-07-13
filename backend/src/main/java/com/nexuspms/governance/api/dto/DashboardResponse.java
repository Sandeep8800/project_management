package com.nexuspms.governance.api.dto;

/** PRD FR-16: consolidated admin dashboard view. */
public record DashboardResponse(long activeProjectCount, long archivedProjectCount, long totalUserCount) {
}

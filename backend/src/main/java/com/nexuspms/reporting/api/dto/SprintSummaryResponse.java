package com.nexuspms.reporting.api.dto;

import com.nexuspms.reporting.domain.SprintSummary;

import java.math.BigDecimal;
import java.util.UUID;

public record SprintSummaryResponse(UUID sprintId, BigDecimal plannedPoints, BigDecimal completedPoints,
                                     BigDecimal scopeAddedPoints, BigDecimal scopeRemovedPoints, int carryOverIssueCount) {
    public static SprintSummaryResponse from(SprintSummary s) {
        return new SprintSummaryResponse(s.getSprintId(), s.getPlannedPoints(), s.getCompletedPoints(),
                s.getScopeAddedPoints(), s.getScopeRemovedPoints(), s.getCarryOverIssueCount());
    }
}

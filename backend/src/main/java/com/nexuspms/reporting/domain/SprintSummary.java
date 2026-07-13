package com.nexuspms.reporting.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * PRD FR-37 / Database Design S8.4.
 *
 * Known simplification: scope_added/scope_removed_points are not tracked in this
 * pass -- true scope-change tracking requires an audit trail of backlog<->sprint
 * moves during a sprint's active window, which isn't built. Both default to 0.
 * committed_points is approximated as the sum of story points for issues
 * attached to the sprint at completion time, not a true point-in-time snapshot
 * taken at sprint start -- flagged as a follow-up for accuracy.
 */
@Entity
@Table(name = "sprint_summaries")
public class SprintSummary {

    @Id
    @Column(name = "sprint_id")
    private UUID sprintId;

    @Column(name = "planned_points", nullable = false, precision = 6, scale = 1)
    private BigDecimal plannedPoints;

    @Column(name = "completed_points", nullable = false, precision = 6, scale = 1)
    private BigDecimal completedPoints;

    @Column(name = "scope_added_points", nullable = false, precision = 6, scale = 1)
    private BigDecimal scopeAddedPoints = BigDecimal.ZERO;

    @Column(name = "scope_removed_points", nullable = false, precision = 6, scale = 1)
    private BigDecimal scopeRemovedPoints = BigDecimal.ZERO;

    @Column(name = "carry_over_issue_count", nullable = false)
    private int carryOverIssueCount;

    protected SprintSummary() {
        // JPA
    }

    public SprintSummary(UUID sprintId, BigDecimal plannedPoints, BigDecimal completedPoints, int carryOverIssueCount) {
        this.sprintId = sprintId;
        this.plannedPoints = plannedPoints;
        this.completedPoints = completedPoints;
        this.carryOverIssueCount = carryOverIssueCount;
    }

    public UUID getSprintId() {
        return sprintId;
    }

    public BigDecimal getPlannedPoints() {
        return plannedPoints;
    }

    public BigDecimal getCompletedPoints() {
        return completedPoints;
    }

    public BigDecimal getScopeAddedPoints() {
        return scopeAddedPoints;
    }

    public BigDecimal getScopeRemovedPoints() {
        return scopeRemovedPoints;
    }

    public int getCarryOverIssueCount() {
        return carryOverIssueCount;
    }
}

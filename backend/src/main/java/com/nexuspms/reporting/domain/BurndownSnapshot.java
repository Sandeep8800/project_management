package com.nexuspms.reporting.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** PRD FR-34 / Database Design S8.1: write target ONLY for ProjectionUpdateService -- never written by request-handling controllers directly (HLD S5). */
@Entity
@Table(name = "burndown_snapshots")
public class BurndownSnapshot {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "sprint_id", nullable = false)
    private UUID sprintId;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "remaining_points", nullable = false, precision = 6, scale = 1)
    private BigDecimal remainingPoints;

    @Column(name = "remaining_issue_count", nullable = false)
    private int remainingIssueCount;

    protected BurndownSnapshot() {
        // JPA
    }

    public BurndownSnapshot(UUID sprintId, LocalDate snapshotDate, BigDecimal remainingPoints, int remainingIssueCount) {
        this.sprintId = sprintId;
        this.snapshotDate = snapshotDate;
        this.remainingPoints = remainingPoints;
        this.remainingIssueCount = remainingIssueCount;
    }

    public void update(BigDecimal remainingPoints, int remainingIssueCount) {
        this.remainingPoints = remainingPoints;
        this.remainingIssueCount = remainingIssueCount;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSprintId() {
        return sprintId;
    }

    public LocalDate getSnapshotDate() {
        return snapshotDate;
    }

    public BigDecimal getRemainingPoints() {
        return remainingPoints;
    }

    public int getRemainingIssueCount() {
        return remainingIssueCount;
    }
}

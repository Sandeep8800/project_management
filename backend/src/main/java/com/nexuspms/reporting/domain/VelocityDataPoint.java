package com.nexuspms.reporting.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

/** PRD FR-35 / Database Design S8.2. */
@Entity
@Table(name = "velocity_data_points")
public class VelocityDataPoint {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "sprint_id", nullable = false, unique = true)
    private UUID sprintId;

    @Column(name = "committed_points", nullable = false, precision = 6, scale = 1)
    private BigDecimal committedPoints;

    @Column(name = "completed_points", nullable = false, precision = 6, scale = 1)
    private BigDecimal completedPoints;

    protected VelocityDataPoint() {
        // JPA
    }

    public VelocityDataPoint(UUID projectId, UUID sprintId, BigDecimal committedPoints, BigDecimal completedPoints) {
        this.projectId = projectId;
        this.sprintId = sprintId;
        this.committedPoints = committedPoints;
        this.completedPoints = completedPoints;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public UUID getSprintId() {
        return sprintId;
    }

    public BigDecimal getCommittedPoints() {
        return committedPoints;
    }

    public BigDecimal getCompletedPoints() {
        return completedPoints;
    }
}

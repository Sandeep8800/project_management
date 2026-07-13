package com.nexuspms.sprintboard.domain;

import com.nexuspms.common.exception.GovernanceSafeguardException;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * LLD S6.1 sprint state machine. The single-active-sprint-per-project invariant
 * (HLD S1, v1 scope) is enforced two ways: the DB partial unique index
 * (Database Design S7.1, the real guarantee under concurrency) and this
 * entity's own state check (fails fast with a clear message before ever
 * reaching the DB for the common case).
 */
@Entity
@Table(name = "sprints")
public class Sprint {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "text")
    private String goal;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SprintStatus status = SprintStatus.PLANNED;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Sprint() {
        // JPA
    }

    public Sprint(UUID projectId, String name, String goal) {
        this.projectId = projectId;
        this.name = name;
        this.goal = goal;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void editPlan(String name, String goal) {
        if (status != SprintStatus.PLANNED) {
            throw new GovernanceSafeguardException("Sprint " + this.name + " can only be edited while PLANNED.");
        }
        if (name != null) this.name = name;
        if (goal != null) this.goal = goal;
    }

    public void start(LocalDate startDate, LocalDate endDate) {
        if (status != SprintStatus.PLANNED) {
            throw new GovernanceSafeguardException("Sprint " + name + " is not in PLANNED state.");
        }
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = SprintStatus.ACTIVE;
    }

    public void complete() {
        if (status != SprintStatus.ACTIVE) {
            throw new GovernanceSafeguardException("Sprint " + name + " is not ACTIVE.");
        }
        this.status = SprintStatus.COMPLETED;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public String getName() {
        return name;
    }

    public String getGoal() {
        return goal;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public SprintStatus getStatus() {
        return status;
    }

    public long getVersion() {
        return version;
    }
}

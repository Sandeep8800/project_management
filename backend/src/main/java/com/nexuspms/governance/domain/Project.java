package com.nexuspms.governance.domain;

import com.nexuspms.common.exception.GovernanceSafeguardException;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** PRD FR-6..9: created only by an admin. Delete requires archive-first (FR-8, LLD S4.3). */
@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "project_key", nullable = false, unique = true)
    private String projectKey;

    @Column(nullable = false)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectMethodology methodology;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "target_release_date")
    private LocalDate targetReleaseDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectStatus status = ProjectStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Project() {
        // JPA
    }

    public Project(String projectKey, String name, String description, ProjectMethodology methodology,
                    LocalDate startDate, LocalDate targetReleaseDate) {
        this.projectKey = projectKey;
        this.name = name;
        this.description = description;
        this.methodology = methodology;
        this.startDate = startDate;
        this.targetReleaseDate = targetReleaseDate;
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

    public void archive() {
        this.status = ProjectStatus.ARCHIVED;
    }

    /** PRD FR-8: archive-before-delete safeguard, enforced here rather than only at the UI layer. */
    public void markDeleted() {
        if (this.status != ProjectStatus.ARCHIVED) {
            throw new GovernanceSafeguardException(
                    "Project " + projectKey + " must be archived before it can be deleted.");
        }
        this.status = ProjectStatus.DELETED;
    }

    public UUID getId() {
        return id;
    }

    public String getProjectKey() {
        return projectKey;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public ProjectMethodology getMethodology() {
        return methodology;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getTargetReleaseDate() {
        return targetReleaseDate;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

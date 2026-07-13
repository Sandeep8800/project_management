package com.nexuspms.backlog.domain;

import com.nexuspms.common.exception.GovernanceSafeguardException;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * LLD S5 / Database Design S6.1: single-table design across all five issue types.
 * project_id/assignee_id/reporter_id/sprint_id/parent_issue_id are raw UUID
 * columns, not JPA relations, deliberately -- this module never reaches into
 * another module's entities directly (LLD S2), and parent/child issue linkage
 * is resolved through IssueRepository, not object-graph navigation.
 *
 * search_vector is DB-generated (Postgres GENERATED ALWAYS AS ... STORED) and is
 * intentionally NOT mapped here -- Hibernate must never attempt to write it.
 */
@Entity
@Table(name = "issues")
public class Issue {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "issue_key", nullable = false, unique = true)
    private String issueKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "issue_type", nullable = false)
    private IssueType issueType;

    @Column(name = "parent_issue_id")
    private UUID parentIssueId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private String status;

    @Column(name = "assignee_id")
    private UUID assigneeId;

    @Column(name = "reporter_id", nullable = false)
    private UUID reporterId;

    @Column(nullable = false)
    private String priority;

    @Column(name = "story_points", precision = 4, scale = 1)
    private BigDecimal storyPoints;

    @Column(name = "sprint_id")
    private UUID sprintId;

    @Column(name = "backlog_rank", nullable = false)
    private BigDecimal backlogRank;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Issue() {
        // JPA
    }

    public Issue(UUID projectId, String issueKey, IssueType issueType, UUID parentIssueId, String title,
                 String description, String initialStatus, UUID reporterId, String priority,
                 BigDecimal storyPoints, BigDecimal backlogRank) {
        this.projectId = projectId;
        this.issueKey = issueKey;
        this.issueType = issueType;
        this.parentIssueId = parentIssueId;
        this.title = title;
        this.description = description;
        this.status = initialStatus;
        this.reporterId = reporterId;
        this.priority = priority;
        this.storyPoints = storyPoints;
        this.backlogRank = backlogRank;
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

    public void applyEdit(String title, String description, String priority, BigDecimal storyPoints, UUID assigneeId) {
        if (title != null) this.title = title;
        if (description != null) this.description = description;
        if (priority != null) this.priority = priority;
        if (storyPoints != null) this.storyPoints = storyPoints;
        if (assigneeId != null) this.assigneeId = assigneeId;
    }

    public void transitionTo(String newStatus) {
        this.status = newStatus;
    }

    public void assignToSprint(UUID sprintId) {
        this.sprintId = sprintId;
    }

    public void removeFromSprint() {
        this.sprintId = null;
    }

    public void moveBacklogRank(BigDecimal newRank) {
        this.backlogRank = newRank;
    }

    /** LLD S5: Sub-task must reference a Task or Story parent; enforced at the service layer using this + IssueType lookups, not a DB constraint (issue types of parent/child aren't statically known at the FK level). */
    public void requireParentFor(IssueType type) {
        if (type == IssueType.SUBTASK && parentIssueId == null) {
            throw new GovernanceSafeguardException("A Sub-task must have a parent Task or Story.");
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public String getIssueKey() {
        return issueKey;
    }

    public IssueType getIssueType() {
        return issueType;
    }

    public UUID getParentIssueId() {
        return parentIssueId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }

    public UUID getAssigneeId() {
        return assigneeId;
    }

    public UUID getReporterId() {
        return reporterId;
    }

    public String getPriority() {
        return priority;
    }

    public BigDecimal getStoryPoints() {
        return storyPoints;
    }

    public UUID getSprintId() {
        return sprintId;
    }

    public BigDecimal getBacklogRank() {
        return backlogRank;
    }

    public long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

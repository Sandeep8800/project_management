package com.nexuspms.governance.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** PRD FR-10/FR-11: exactly one role per user per project -- project-scoped, not global (Database Design S5.2 unique (user_id, project_id)). */
@Entity
@Table(name = "project_memberships")
public class ProjectMembership {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    @Column(name = "assigned_by", nullable = false)
    private UUID assignedBy;

    protected ProjectMembership() {
        // JPA
    }

    public ProjectMembership(UUID userId, UUID projectId, Role role, UUID assignedBy) {
        this.userId = userId;
        this.projectId = projectId;
        this.role = role;
        this.assignedBy = assignedBy;
        this.assignedAt = Instant.now();
    }

    public void changeRole(Role newRole) {
        this.role = newRole;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public Role getRole() {
        return role;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }
}

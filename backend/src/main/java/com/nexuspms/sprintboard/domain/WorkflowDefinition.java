package com.nexuspms.sprintboard.domain;

import jakarta.persistence.*;

import java.util.UUID;

/** LLD S6.2: one workflow definition per project in v1 (Database Design S7.2). */
@Entity
@Table(name = "workflow_definitions")
public class WorkflowDefinition {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "project_id", nullable = false, unique = true)
    private UUID projectId;

    protected WorkflowDefinition() {
        // JPA
    }

    public WorkflowDefinition(UUID projectId) {
        this.projectId = projectId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProjectId() {
        return projectId;
    }
}

package com.nexuspms.sprintboard.domain;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "workflow_transitions")
public class WorkflowTransition {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "workflow_definition_id", nullable = false)
    private UUID workflowDefinitionId;

    @Column(name = "from_status_id", nullable = false)
    private UUID fromStatusId;

    @Column(name = "to_status_id", nullable = false)
    private UUID toStatusId;

    protected WorkflowTransition() {
        // JPA
    }

    public WorkflowTransition(UUID workflowDefinitionId, UUID fromStatusId, UUID toStatusId) {
        this.workflowDefinitionId = workflowDefinitionId;
        this.fromStatusId = fromStatusId;
        this.toStatusId = toStatusId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getWorkflowDefinitionId() {
        return workflowDefinitionId;
    }

    public UUID getFromStatusId() {
        return fromStatusId;
    }

    public UUID getToStatusId() {
        return toStatusId;
    }
}

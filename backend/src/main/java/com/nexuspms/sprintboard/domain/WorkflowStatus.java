package com.nexuspms.sprintboard.domain;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "workflow_statuses")
public class WorkflowStatus {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "workflow_definition_id", nullable = false)
    private UUID workflowDefinitionId;

    @Column(nullable = false)
    private String name;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "is_initial", nullable = false)
    private boolean isInitial;

    @Column(name = "is_terminal", nullable = false)
    private boolean isTerminal;

    protected WorkflowStatus() {
        // JPA
    }

    public WorkflowStatus(UUID workflowDefinitionId, String name, int displayOrder, boolean isInitial, boolean isTerminal) {
        this.workflowDefinitionId = workflowDefinitionId;
        this.name = name;
        this.displayOrder = displayOrder;
        this.isInitial = isInitial;
        this.isTerminal = isTerminal;
    }

    public UUID getId() {
        return id;
    }

    public UUID getWorkflowDefinitionId() {
        return workflowDefinitionId;
    }

    public String getName() {
        return name;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public boolean isInitial() {
        return isInitial;
    }

    public boolean isTerminal() {
        return isTerminal;
    }
}

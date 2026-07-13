package com.nexuspms.sprintboard.api.dto;

import com.nexuspms.sprintboard.domain.WorkflowStatus;

import java.util.UUID;

public record WorkflowStatusResponse(UUID id, String name, int displayOrder, boolean isInitial, boolean isTerminal) {
    public static WorkflowStatusResponse from(WorkflowStatus s) {
        return new WorkflowStatusResponse(s.getId(), s.getName(), s.getDisplayOrder(), s.isInitial(), s.isTerminal());
    }
}

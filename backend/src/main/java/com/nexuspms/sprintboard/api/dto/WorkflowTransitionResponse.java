package com.nexuspms.sprintboard.api.dto;

import com.nexuspms.sprintboard.domain.WorkflowTransition;

import java.util.UUID;

public record WorkflowTransitionResponse(UUID id, UUID fromStatusId, UUID toStatusId) {
    public static WorkflowTransitionResponse from(WorkflowTransition t) {
        return new WorkflowTransitionResponse(t.getId(), t.getFromStatusId(), t.getToStatusId());
    }
}

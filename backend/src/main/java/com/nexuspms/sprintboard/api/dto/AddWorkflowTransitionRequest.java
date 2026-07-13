package com.nexuspms.sprintboard.api.dto;

import jakarta.validation.constraints.NotBlank;

public record AddWorkflowTransitionRequest(@NotBlank String fromStatus, @NotBlank String toStatus) {
}

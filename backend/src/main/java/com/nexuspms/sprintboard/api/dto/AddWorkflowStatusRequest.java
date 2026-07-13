package com.nexuspms.sprintboard.api.dto;

import jakarta.validation.constraints.NotBlank;

public record AddWorkflowStatusRequest(@NotBlank String name, int displayOrder, boolean isInitial, boolean isTerminal) {
}

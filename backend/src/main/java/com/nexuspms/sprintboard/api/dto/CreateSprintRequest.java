package com.nexuspms.sprintboard.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateSprintRequest(@NotBlank String name, String goal) {
}

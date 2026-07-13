package com.nexuspms.sprintboard.api.dto;

import jakarta.validation.constraints.NotBlank;

public record TransitionRequest(@NotBlank String targetStatus) {
}

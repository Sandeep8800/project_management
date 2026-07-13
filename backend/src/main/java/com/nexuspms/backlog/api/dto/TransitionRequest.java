package com.nexuspms.backlog.api.dto;

import jakarta.validation.constraints.NotBlank;

public record TransitionRequest(@NotBlank String targetStatus) {
}

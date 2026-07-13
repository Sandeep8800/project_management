package com.nexuspms.backlog.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateLabelRequest(@NotBlank String name) {
}

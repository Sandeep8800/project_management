package com.nexuspms.backlog.api.dto;

import jakarta.validation.constraints.NotBlank;

public record GenerateUploadUrlRequest(@NotBlank String fileName) {
}

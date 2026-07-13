package com.nexuspms.backlog.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record RegisterAttachmentRequest(
        @NotBlank String fileName,
        @NotBlank String contentType,
        @Positive long sizeBytes,
        @NotBlank String storageKey
) {
}

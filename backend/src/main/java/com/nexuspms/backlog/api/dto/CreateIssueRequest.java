package com.nexuspms.backlog.api.dto;

import com.nexuspms.backlog.domain.IssueType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateIssueRequest(
        @NotNull IssueType issueType,
        UUID parentIssueId,
        @NotBlank String title,
        String description,
        @NotBlank String priority,
        BigDecimal storyPoints,
        UUID assigneeId
) {
}

package com.nexuspms.backlog.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReorderRequest(@NotNull UUID issueId, UUID afterIssueId, UUID beforeIssueId) {
}

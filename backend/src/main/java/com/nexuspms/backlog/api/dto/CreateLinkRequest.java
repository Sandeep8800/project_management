package com.nexuspms.backlog.api.dto;

import com.nexuspms.backlog.domain.IssueLinkType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateLinkRequest(@NotNull UUID targetIssueId, @NotNull IssueLinkType linkType) {
}

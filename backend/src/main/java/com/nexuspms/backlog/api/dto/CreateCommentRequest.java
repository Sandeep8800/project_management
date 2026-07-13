package com.nexuspms.backlog.api.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.UUID;

public record CreateCommentRequest(@NotBlank String body, List<UUID> mentionedUserIds) {
}

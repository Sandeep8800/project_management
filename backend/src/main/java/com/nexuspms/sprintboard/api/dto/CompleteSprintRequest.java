package com.nexuspms.sprintboard.api.dto;

import com.nexuspms.sprintboard.service.RolloverDecision;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** PRD FR-25: rolloverDecision is required, never defaulted. */
public record CompleteSprintRequest(@NotNull RolloverDecision rolloverDecision, UUID targetSprintId) {
}

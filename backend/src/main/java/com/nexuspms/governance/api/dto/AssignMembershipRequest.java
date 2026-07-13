package com.nexuspms.governance.api.dto;

import com.nexuspms.governance.domain.Role;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignMembershipRequest(@NotNull UUID userId, @NotNull Role role) {
}

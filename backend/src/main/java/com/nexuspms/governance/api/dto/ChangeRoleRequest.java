package com.nexuspms.governance.api.dto;

import com.nexuspms.governance.domain.Role;
import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequest(@NotNull Role role) {
}

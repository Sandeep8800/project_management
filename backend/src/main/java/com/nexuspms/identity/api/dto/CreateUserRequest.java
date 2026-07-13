package com.nexuspms.identity.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** PRD FR-1: admin-only creation. No public registration DTO exists anywhere in this API. */
public record CreateUserRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        String employeeId,
        String department,
        @NotBlank String defaultRole,
        String initialPassword
) {
}

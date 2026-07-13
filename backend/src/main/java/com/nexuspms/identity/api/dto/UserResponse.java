package com.nexuspms.identity.api.dto;

import com.nexuspms.identity.domain.User;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String email,
        String employeeId,
        String department,
        String defaultRole,
        String status,
        boolean platformAdmin,
        boolean emailNotificationsEnabled,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(), user.getName(), user.getEmail(), user.getEmployeeId(),
                user.getDepartment(), user.getDefaultRole(), user.getStatus().name(),
                user.isPlatformAdmin(), user.isEmailNotificationsEnabled(), user.getCreatedAt());
    }
}

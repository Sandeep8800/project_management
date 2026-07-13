package com.nexuspms.governance.api.dto;

import com.nexuspms.governance.domain.ProjectMembership;

import java.time.Instant;
import java.util.UUID;

public record MembershipResponse(UUID userId, UUID projectId, String role, Instant assignedAt) {
    public static MembershipResponse from(ProjectMembership m) {
        return new MembershipResponse(m.getUserId(), m.getProjectId(), m.getRole().name(), m.getAssignedAt());
    }
}

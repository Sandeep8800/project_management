package com.nexuspms.governance.api.dto;

import com.nexuspms.governance.domain.Project;

import java.time.LocalDate;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        String projectKey,
        String name,
        String description,
        String methodology,
        LocalDate startDate,
        LocalDate targetReleaseDate,
        String status
) {
    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
                project.getId(), project.getProjectKey(), project.getName(), project.getDescription(),
                project.getMethodology().name(), project.getStartDate(), project.getTargetReleaseDate(),
                project.getStatus().name());
    }
}

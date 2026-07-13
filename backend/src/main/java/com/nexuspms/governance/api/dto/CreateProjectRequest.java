package com.nexuspms.governance.api.dto;

import com.nexuspms.governance.domain.ProjectMethodology;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateProjectRequest(
        @NotBlank String projectKey,
        @NotBlank String name,
        String description,
        @NotNull ProjectMethodology methodology,
        @NotNull LocalDate startDate,
        LocalDate targetReleaseDate
) {
}

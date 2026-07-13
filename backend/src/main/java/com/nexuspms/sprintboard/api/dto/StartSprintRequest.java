package com.nexuspms.sprintboard.api.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record StartSprintRequest(@NotNull LocalDate startDate, @NotNull LocalDate endDate) {
}

package com.nexuspms.sprintboard.api.dto;

import com.nexuspms.sprintboard.domain.Sprint;

import java.time.LocalDate;
import java.util.UUID;

public record SprintResponse(UUID id, UUID projectId, String name, String goal, LocalDate startDate,
                              LocalDate endDate, String status, long version) {
    public static SprintResponse from(Sprint sprint) {
        return new SprintResponse(sprint.getId(), sprint.getProjectId(), sprint.getName(), sprint.getGoal(),
                sprint.getStartDate(), sprint.getEndDate(), sprint.getStatus().name(), sprint.getVersion());
    }
}

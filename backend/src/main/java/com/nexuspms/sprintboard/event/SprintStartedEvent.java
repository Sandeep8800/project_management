package com.nexuspms.sprintboard.event;

import java.util.UUID;

public record SprintStartedEvent(UUID sprintId, UUID projectId) {
}

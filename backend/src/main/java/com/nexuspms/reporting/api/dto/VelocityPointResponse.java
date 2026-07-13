package com.nexuspms.reporting.api.dto;

import com.nexuspms.reporting.domain.VelocityDataPoint;

import java.math.BigDecimal;
import java.util.UUID;

public record VelocityPointResponse(UUID sprintId, BigDecimal committedPoints, BigDecimal completedPoints) {
    public static VelocityPointResponse from(VelocityDataPoint v) {
        return new VelocityPointResponse(v.getSprintId(), v.getCommittedPoints(), v.getCompletedPoints());
    }
}

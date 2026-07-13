package com.nexuspms.reporting.api.dto;

import com.nexuspms.reporting.domain.BurndownSnapshot;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BurndownPointResponse(LocalDate date, BigDecimal remainingPoints, int remainingIssueCount) {
    public static BurndownPointResponse from(BurndownSnapshot s) {
        return new BurndownPointResponse(s.getSnapshotDate(), s.getRemainingPoints(), s.getRemainingIssueCount());
    }
}

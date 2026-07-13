package com.nexuspms.reporting.api.dto;

import com.nexuspms.reporting.domain.CfdSnapshot;

import java.time.LocalDate;

public record CfdPointResponse(LocalDate date, String statusName, int issueCount) {
    public static CfdPointResponse from(CfdSnapshot s) {
        return new CfdPointResponse(s.getSnapshotDate(), s.getStatusName(), s.getIssueCount());
    }
}

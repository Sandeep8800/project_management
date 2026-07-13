package com.nexuspms.reporting.api;

import com.nexuspms.governance.domain.Permission;
import com.nexuspms.governance.security.RequirePermission;
import com.nexuspms.reporting.api.dto.BurndownPointResponse;
import com.nexuspms.reporting.api.dto.CfdPointResponse;
import com.nexuspms.reporting.api.dto.SprintSummaryResponse;
import com.nexuspms.reporting.api.dto.VelocityPointResponse;
import com.nexuspms.reporting.service.ReportQueryService;
import com.nexuspms.sprintboard.domain.BoardType;
import com.nexuspms.sprintboard.service.BoardService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * API Design S8: read-only. No write verbs in this controller at all --
 * structurally enforces PRD FR-38 (Viewer/Stakeholder read-only guarantee)
 * alongside the RBAC check, not instead of it.
 *
 * Coding-phase path amendment (consistent with IssueController's note):
 * sprint-scoped reports are nested under /projects/{projectId}/sprints/{sprintId}
 * rather than the flat /sprints/{sprintId} in API Design S8, so RequirePermission
 * can resolve projectId without a cross-module lookup.
 */
@RestController
@RequestMapping("/projects/{projectId}")
public class ReportController {

    private final ReportQueryService reportQueryService;
    private final BoardService boardService;

    public ReportController(ReportQueryService reportQueryService, BoardService boardService) {
        this.reportQueryService = reportQueryService;
        this.boardService = boardService;
    }

    @GetMapping("/sprints/{sprintId}/reports/burndown")
    @RequirePermission(Permission.VIEW_REPORTS)
    public List<BurndownPointResponse> burndown(@PathVariable UUID projectId, @PathVariable UUID sprintId) {
        return reportQueryService.burndown(sprintId).stream().map(BurndownPointResponse::from).toList();
    }

    @GetMapping("/reports/velocity")
    @RequirePermission(Permission.VIEW_REPORTS)
    public List<VelocityPointResponse> velocity(@PathVariable UUID projectId) {
        return reportQueryService.velocity(projectId).stream().map(VelocityPointResponse::from).toList();
    }

    @GetMapping("/boards/{boardType}/reports/cfd")
    @RequirePermission(Permission.VIEW_REPORTS)
    public List<CfdPointResponse> cfd(@PathVariable UUID projectId, @PathVariable BoardType boardType,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        UUID boardId = boardService.getBoard(projectId, boardType).getId();
        return reportQueryService.cfd(boardId, from, to).stream().map(CfdPointResponse::from).toList();
    }

    @GetMapping("/sprints/{sprintId}/reports/summary")
    @RequirePermission(Permission.VIEW_REPORTS)
    public SprintSummaryResponse summary(@PathVariable UUID projectId, @PathVariable UUID sprintId) {
        return reportQueryService.sprintSummary(sprintId).map(SprintSummaryResponse::from)
                .orElseThrow(() -> new com.nexuspms.common.exception.ResourceNotFoundException(
                        "No summary yet for sprint " + sprintId + " (available after the sprint completes)."));
    }
}

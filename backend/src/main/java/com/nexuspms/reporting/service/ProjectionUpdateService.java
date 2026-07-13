package com.nexuspms.reporting.service;

import com.nexuspms.backlog.domain.Issue;
import com.nexuspms.backlog.event.IssueCreatedEvent;
import com.nexuspms.backlog.event.IssueStatusChangedEvent;
import com.nexuspms.backlog.service.IssueService;
import com.nexuspms.reporting.domain.BurndownSnapshot;
import com.nexuspms.reporting.domain.CfdSnapshot;
import com.nexuspms.reporting.repository.BurndownSnapshotRepository;
import com.nexuspms.reporting.repository.CfdSnapshotRepository;
import com.nexuspms.sprintboard.domain.Board;
import com.nexuspms.sprintboard.domain.Sprint;
import com.nexuspms.sprintboard.domain.SprintStatus;
import com.nexuspms.sprintboard.event.SprintStartedEvent;
import com.nexuspms.sprintboard.repository.BoardRepository;
import com.nexuspms.sprintboard.repository.SprintRepository;
import com.nexuspms.sprintboard.service.WorkflowService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * LLD S7: reacts to issue/sprint domain events and maintains the projection
 * tables Reporting serves reads from.
 *
 * Known simplification vs. the HLD S5/S8.2 "incremental, delta-based
 * projection" design intent: this recomputes the full remaining-points/CFD
 * count on every relevant event rather than applying a true incremental delta.
 * Correct, and cheap enough at this pass's scale, but not the O(1)-per-event
 * update the HLD envisioned for the Large-scale target -- flagged as a
 * follow-up optimization, not built here.
 */
@Service
public class ProjectionUpdateService {

    private final IssueService issueService;
    private final WorkflowService workflowService;
    private final SprintRepository sprintRepository;
    private final BoardRepository boardRepository;
    private final BurndownSnapshotRepository burndownSnapshotRepository;
    private final CfdSnapshotRepository cfdSnapshotRepository;

    public ProjectionUpdateService(IssueService issueService, WorkflowService workflowService,
                                    SprintRepository sprintRepository, BoardRepository boardRepository,
                                    BurndownSnapshotRepository burndownSnapshotRepository,
                                    CfdSnapshotRepository cfdSnapshotRepository) {
        this.issueService = issueService;
        this.workflowService = workflowService;
        this.sprintRepository = sprintRepository;
        this.boardRepository = boardRepository;
        this.burndownSnapshotRepository = burndownSnapshotRepository;
        this.cfdSnapshotRepository = cfdSnapshotRepository;
    }

    @EventListener
    @Transactional
    public void onIssueStatusChanged(IssueStatusChangedEvent event) {
        if (event.sprintId() != null) {
            updateBurndown(event.sprintId());
        }
        updateCfd(event.projectId());
    }

    @EventListener
    @Transactional
    public void onIssueCreated(IssueCreatedEvent event) {
        updateCfd(event.projectId());
    }

    @EventListener
    @Transactional
    public void onSprintStarted(SprintStartedEvent event) {
        updateBurndown(event.sprintId());
    }

    /** LLD S7 backfill/repair: recomputes CFD + active-sprint burndown for a project from current state. Used by ProjectionRebuildJobHandler (LLD S10), not on the request path. */
    @Transactional
    public void rebuildForProject(java.util.UUID projectId) {
        updateCfd(projectId);
        sprintRepository.findByProjectIdAndStatus(projectId, SprintStatus.ACTIVE)
                .forEach(sprint -> updateBurndown(sprint.getId()));
    }

    private void updateBurndown(java.util.UUID sprintId) {
        Sprint sprint = sprintRepository.findById(sprintId).orElse(null);
        if (sprint == null || sprint.getStatus() != SprintStatus.ACTIVE) {
            return; // PRD FR-34: burndown only tracked for the active sprint
        }
        List<String> terminal = workflowService.terminalStatusNames(sprint.getProjectId());
        List<Issue> remaining = issueService.listIncompleteInSprint(sprintId, terminal);
        BigDecimal remainingPoints = remaining.stream()
                .map(Issue::getStoryPoints).filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDate today = LocalDate.now();
        burndownSnapshotRepository.findBySprintIdAndSnapshotDate(sprintId, today)
                .ifPresentOrElse(
                        s -> s.update(remainingPoints, remaining.size()),
                        () -> burndownSnapshotRepository.save(new BurndownSnapshot(sprintId, today, remainingPoints, remaining.size())));
    }

    private void updateCfd(java.util.UUID projectId) {
        List<Issue> allIssues = issueService.listByProject(projectId);
        Map<String, Long> countsByStatus = allIssues.stream()
                .collect(Collectors.groupingBy(Issue::getStatus, Collectors.counting()));

        LocalDate today = LocalDate.now();
        for (Board board : boardRepository.findByProjectId(projectId)) {
            for (Map.Entry<String, Long> entry : countsByStatus.entrySet()) {
                cfdSnapshotRepository.findByBoardIdAndSnapshotDateAndStatusName(board.getId(), today, entry.getKey())
                        .ifPresentOrElse(
                                s -> s.update(entry.getValue().intValue()),
                                () -> cfdSnapshotRepository.save(new CfdSnapshot(board.getId(), today, entry.getKey(), entry.getValue().intValue())));
            }
        }
    }
}

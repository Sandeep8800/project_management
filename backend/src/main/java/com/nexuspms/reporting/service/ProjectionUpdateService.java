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
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * LLD S7: reacts to issue/sprint domain events and maintains the projection
 * tables Reporting serves reads from.
 *
 * CFD updates are a true incremental delta (decrement the from-status count,
 * increment the to-status count) rather than a full per-project rescan -- the
 * O(1)-per-event update the HLD S5/S8.2 design intent called for. A brand-new
 * "today" row is seeded from the most recent prior day's count (carry-forward)
 * rather than starting at zero, since a CFD snapshot represents cumulative
 * state, not a same-day-only count. Burndown remains a targeted per-sprint
 * recompute (bounded by sprint size, not project size, so it was never the
 * expensive path this optimization targets).
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
        adjustCfd(event.projectId(), event.fromStatus(), -1);
        adjustCfd(event.projectId(), event.toStatus(), 1);
    }

    @EventListener
    @Transactional
    public void onIssueCreated(IssueCreatedEvent event) {
        String initialStatus = issueService.get(event.issueId()).getStatus();
        adjustCfd(event.projectId(), initialStatus, 1);
    }

    @EventListener
    @Transactional
    public void onSprintStarted(SprintStartedEvent event) {
        updateBurndown(event.sprintId());
    }

    /** LLD S7 backfill/repair: recomputes CFD + active-sprint burndown for a project from current state. Used by ProjectionRebuildJobHandler (LLD S10), not on the request path -- this IS a full rescan, deliberately, since its entire purpose is correcting drift the incremental path may have accumulated. */
    @Transactional
    public void rebuildForProject(UUID projectId) {
        rescanCfd(projectId);
        sprintRepository.findByProjectIdAndStatus(projectId, SprintStatus.ACTIVE)
                .forEach(sprint -> updateBurndown(sprint.getId()));
    }

    private void updateBurndown(UUID sprintId) {
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

    /** Incremental: adjusts today's count for one status on every one of the project's boards, seeding from the prior day's count if today has no row yet. */
    private void adjustCfd(UUID projectId, String statusName, int delta) {
        LocalDate today = LocalDate.now();
        for (Board board : boardRepository.findByProjectId(projectId)) {
            CfdSnapshot snapshot = cfdSnapshotRepository.findByBoardIdAndSnapshotDateAndStatusName(board.getId(), today, statusName)
                    .orElseGet(() -> {
                        int carriedForward = cfdSnapshotRepository
                                .findFirstByBoardIdAndStatusNameAndSnapshotDateLessThanOrderBySnapshotDateDesc(board.getId(), statusName, today)
                                .map(CfdSnapshot::getIssueCount)
                                .orElse(0);
                        return cfdSnapshotRepository.save(new CfdSnapshot(board.getId(), today, statusName, carriedForward));
                    });
            snapshot.update(Math.max(0, snapshot.getIssueCount() + delta));
        }
    }

    /** Full rescan, used only by rebuildForProject -- the deliberate exception to the incremental-by-default rule above. */
    private void rescanCfd(UUID projectId) {
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

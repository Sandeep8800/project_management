package com.nexuspms.sprintboard.service;

import com.nexuspms.backlog.domain.Issue;
import com.nexuspms.backlog.service.IssueService;
import com.nexuspms.common.event.DomainEventPublisher;
import com.nexuspms.common.exception.ConcurrentModificationException;
import com.nexuspms.common.exception.GovernanceSafeguardException;
import com.nexuspms.common.exception.ResourceNotFoundException;
import com.nexuspms.common.job.BackgroundJobService;
import com.nexuspms.sprintboard.domain.Sprint;
import com.nexuspms.sprintboard.domain.SprintStatus;
import com.nexuspms.sprintboard.event.SprintCompletedEvent;
import com.nexuspms.sprintboard.event.SprintStartedEvent;
import com.nexuspms.sprintboard.repository.SprintRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * LLD S6.1: PLANNED -> ACTIVE -> COMPLETED. Single-active-sprint-per-project is
 * enforced first here (fast, clear error) and again by the DB partial unique
 * index (Database Design S7.1, the real guarantee under a race this
 * application-level check could still lose).
 */
@Service
public class SprintService {

    /** Above this many incomplete issues, rollover is offloaded to a background job (HLD S8.5) so complete() doesn't block on bulk re-parenting. */
    private static final int INLINE_ROLLOVER_THRESHOLD = 50;

    public static final String SPRINT_ROLLOVER_JOB_TYPE = "SPRINT_ROLLOVER";

    private final SprintRepository sprintRepository;
    private final IssueService issueService;
    private final WorkflowService workflowService;
    private final DomainEventPublisher eventPublisher;
    private final BackgroundJobService backgroundJobService;

    public SprintService(SprintRepository sprintRepository, IssueService issueService, WorkflowService workflowService,
                          DomainEventPublisher eventPublisher, BackgroundJobService backgroundJobService) {
        this.sprintRepository = sprintRepository;
        this.issueService = issueService;
        this.workflowService = workflowService;
        this.eventPublisher = eventPublisher;
        this.backgroundJobService = backgroundJobService;
    }

    @Transactional
    public Sprint create(UUID projectId, String name, String goal) {
        return sprintRepository.save(new Sprint(projectId, name, goal));
    }

    public Sprint get(UUID sprintId) {
        return sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint " + sprintId + " not found."));
    }

    public List<Sprint> list(UUID projectId, SprintStatus status) {
        return status == null ? sprintRepository.findByProjectId(projectId) : sprintRepository.findByProjectIdAndStatus(projectId, status);
    }

    @Transactional
    public Sprint edit(UUID sprintId, long expectedVersion, String name, String goal) {
        Sprint sprint = getWithVersionCheck(sprintId, expectedVersion);
        sprint.editPlan(name, goal);
        return sprint;
    }

    /** PRD FR-23 / LLD S6.1: fails with a clear GovernanceSafeguardException if the project already has an active sprint; the DB partial unique index is the final backstop under a race. */
    @Transactional
    public Sprint start(UUID sprintId, long expectedVersion, LocalDate startDate, LocalDate endDate) {
        Sprint sprint = getWithVersionCheck(sprintId, expectedVersion);
        sprintRepository.findFirstByProjectIdAndStatus(sprint.getProjectId(), SprintStatus.ACTIVE).ifPresent(active -> {
            throw new GovernanceSafeguardException(
                    "Project already has an active sprint (" + active.getName() + "). Complete it before starting another.");
        });
        try {
            sprint.start(startDate, endDate);
            sprintRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new GovernanceSafeguardException("Project already has an active sprint (detected at commit).");
        }
        eventPublisher.publish(new SprintStartedEvent(sprint.getId(), sprint.getProjectId()));
        return sprint;
    }

    /** PRD FR-25: rolloverDecision is a required parameter, never a silent default. */
    @Transactional
    public Sprint complete(UUID sprintId, long expectedVersion, RolloverDecision rolloverDecision, UUID targetSprintId) {
        Sprint sprint = getWithVersionCheck(sprintId, expectedVersion);
        if (rolloverDecision == RolloverDecision.MOVE_TO_NEXT_SPRINT && targetSprintId == null) {
            throw new GovernanceSafeguardException("A target sprint is required when rolling over to the next sprint.");
        }

        List<String> terminalStatuses = workflowService.terminalStatusNames(sprint.getProjectId());
        List<Issue> incomplete = issueService.listIncompleteInSprint(sprintId, terminalStatuses);

        UUID newSprintId = rolloverDecision == RolloverDecision.MOVE_TO_NEXT_SPRINT ? targetSprintId : null;
        if (incomplete.size() <= INLINE_ROLLOVER_THRESHOLD) {
            issueService.moveManyToSprint(incomplete.stream().map(Issue::getId).toList(), newSprintId);
        } else {
            backgroundJobService.enqueue(SPRINT_ROLLOVER_JOB_TYPE, sprintId.toString(), Map.of(
                    "sprintId", sprintId.toString(),
                    "newSprintId", newSprintId == null ? "" : newSprintId.toString()));
        }

        sprint.complete();
        eventPublisher.publish(new SprintCompletedEvent(sprint.getId(), sprint.getProjectId(), incomplete.size()));
        return sprint;
    }

    private Sprint getWithVersionCheck(UUID sprintId, long expectedVersion) {
        Sprint sprint = get(sprintId);
        if (sprint.getVersion() != expectedVersion) {
            throw new ConcurrentModificationException(
                    "Sprint " + sprint.getName() + " has changed since it was last read.", expectedVersion, sprint.getVersion());
        }
        return sprint;
    }
}

package com.nexuspms.reporting.service;

import com.nexuspms.backlog.domain.Issue;
import com.nexuspms.backlog.service.IssueService;
import com.nexuspms.reporting.domain.SprintSummary;
import com.nexuspms.reporting.domain.VelocityDataPoint;
import com.nexuspms.reporting.repository.SprintSummaryRepository;
import com.nexuspms.reporting.repository.VelocityDataPointRepository;
import com.nexuspms.sprintboard.event.SprintCompletedEvent;
import com.nexuspms.sprintboard.service.WorkflowService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/** PRD FR-35/FR-37: composes velocity + sprint-summary projections at SprintCompleted time rather than recomputing on every report view (HLD S8.2). */
@Service
public class SprintSummaryService {

    private final IssueService issueService;
    private final WorkflowService workflowService;
    private final VelocityDataPointRepository velocityDataPointRepository;
    private final SprintSummaryRepository sprintSummaryRepository;

    public SprintSummaryService(IssueService issueService, WorkflowService workflowService,
                                 VelocityDataPointRepository velocityDataPointRepository,
                                 SprintSummaryRepository sprintSummaryRepository) {
        this.issueService = issueService;
        this.workflowService = workflowService;
        this.velocityDataPointRepository = velocityDataPointRepository;
        this.sprintSummaryRepository = sprintSummaryRepository;
    }

    @EventListener
    @Transactional
    public void onSprintCompleted(SprintCompletedEvent event) {
        List<String> terminal = workflowService.terminalStatusNames(event.projectId());
        List<Issue> issuesInSprint = issueService.listBySprint(event.sprintId());

        BigDecimal committed = sumPoints(issuesInSprint);
        BigDecimal completed = sumPoints(issuesInSprint.stream().filter(i -> terminal.contains(i.getStatus())).toList());

        velocityDataPointRepository.findBySprintId(event.sprintId())
                .ifPresentOrElse(
                        existing -> { /* immutable once recorded */ },
                        () -> velocityDataPointRepository.save(new VelocityDataPoint(event.projectId(), event.sprintId(), committed, completed)));

        if (!sprintSummaryRepository.existsById(event.sprintId())) {
            sprintSummaryRepository.save(new SprintSummary(event.sprintId(), committed, completed, event.carryOverIssueCount()));
        }
    }

    private BigDecimal sumPoints(List<Issue> issues) {
        return issues.stream().map(Issue::getStoryPoints).filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

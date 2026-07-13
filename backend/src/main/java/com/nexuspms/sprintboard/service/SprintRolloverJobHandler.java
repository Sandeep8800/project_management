package com.nexuspms.sprintboard.service;

import com.nexuspms.backlog.domain.Issue;
import com.nexuspms.backlog.service.IssueService;
import com.nexuspms.common.job.JobHandler;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/** LLD S10: processes SPRINT_ROLLOVER jobs enqueued by SprintService.complete() for sprints above the inline threshold. Idempotent by sprintId (re-running re-checks each issue's current sprint before re-parenting, since moveManyToSprint is a plain assignment, not additive). */
@Component
public class SprintRolloverJobHandler implements JobHandler {

    private final IssueService issueService;

    public SprintRolloverJobHandler(IssueService issueService) {
        this.issueService = issueService;
    }

    @Override
    public String jobType() {
        return SprintService.SPRINT_ROLLOVER_JOB_TYPE;
    }

    @Override
    public void handle(Map<String, Object> payload) {
        UUID sprintId = UUID.fromString((String) payload.get("sprintId"));
        String newSprintIdRaw = (String) payload.get("newSprintId");
        UUID newSprintId = (newSprintIdRaw == null || newSprintIdRaw.isBlank()) ? null : UUID.fromString(newSprintIdRaw);

        for (Issue issue : issueService.listBySprint(sprintId)) {
            issueService.moveManyToSprint(java.util.List.of(issue.getId()), newSprintId);
        }
    }
}

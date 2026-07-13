package com.nexuspms.sprintboard.api;

import com.nexuspms.backlog.api.dto.IssueResponse;
import com.nexuspms.backlog.domain.Issue;
import com.nexuspms.backlog.service.IssueService;
import com.nexuspms.common.web.IfMatch;
import com.nexuspms.governance.domain.Permission;
import com.nexuspms.governance.security.RequirePermission;
import com.nexuspms.sprintboard.api.dto.TransitionRequest;
import com.nexuspms.sprintboard.domain.BoardType;
import com.nexuspms.sprintboard.service.BoardService;
import com.nexuspms.sprintboard.service.WorkflowTransitionValidator;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * API Design S6 POST .../issues/{issueId}/transitions -- owned by Sprint & Board
 * (not Backlog & Issues) because workflow-legality is this module's own domain
 * (LLD S6.2) and HLD S4 has Sprint & Board depend on Backlog & Issues, never the
 * reverse; putting this endpoint in Backlog would have required the opposite
 * dependency direction.
 */
@RestController
public class IssueTransitionController {

    private final IssueService issueService;
    private final WorkflowTransitionValidator workflowTransitionValidator;
    private final BoardService boardService;

    public IssueTransitionController(IssueService issueService, WorkflowTransitionValidator workflowTransitionValidator,
                                      BoardService boardService) {
        this.issueService = issueService;
        this.workflowTransitionValidator = workflowTransitionValidator;
        this.boardService = boardService;
    }

    @PostMapping("/projects/{projectId}/issues/{issueId}/transitions")
    @RequirePermission(Permission.TRANSITION_STATUS)
    public IssueResponse transition(@PathVariable UUID projectId, @PathVariable UUID issueId,
                                     @RequestHeader("If-Match") String ifMatch,
                                     @Valid @RequestBody TransitionRequest request) {
        Issue current = issueService.get(issueId);
        workflowTransitionValidator.validate(projectId, current.getIssueKey(), current.getStatus(), request.targetStatus());
        boardService.enforceWipLimit(projectId, BoardType.KANBAN, request.targetStatus());

        Issue updated = issueService.transition(issueId, IfMatch.parseVersion(ifMatch), request.targetStatus());
        return IssueResponse.from(updated);
    }
}

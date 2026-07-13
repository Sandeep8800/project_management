package com.nexuspms.sprintboard.api;

import com.nexuspms.governance.domain.Permission;
import com.nexuspms.governance.security.RequireMembership;
import com.nexuspms.governance.security.RequirePermission;
import com.nexuspms.sprintboard.api.dto.*;
import com.nexuspms.sprintboard.service.WorkflowService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** API Design S7: PRD FR-30, per-project configurable workflow. */
@RestController
@RequestMapping("/projects/{projectId}/workflow")
public class WorkflowController {

    private final WorkflowService workflowService;

    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @GetMapping("/statuses")
    @RequireMembership
    public List<WorkflowStatusResponse> statuses(@PathVariable UUID projectId) {
        return workflowService.listStatuses(projectId).stream().map(WorkflowStatusResponse::from).toList();
    }

    @GetMapping("/transitions")
    @RequireMembership
    public List<WorkflowTransitionResponse> transitions(@PathVariable UUID projectId) {
        return workflowService.listTransitions(projectId).stream().map(WorkflowTransitionResponse::from).toList();
    }

    @PostMapping("/statuses")
    @RequirePermission(Permission.CONFIGURE_BOARD)
    public WorkflowStatusResponse addStatus(@PathVariable UUID projectId, @Valid @RequestBody AddWorkflowStatusRequest request) {
        return WorkflowStatusResponse.from(workflowService.addStatus(
                projectId, request.name(), request.displayOrder(), request.isInitial(), request.isTerminal()));
    }

    @PostMapping("/transitions")
    @RequirePermission(Permission.CONFIGURE_BOARD)
    public WorkflowTransitionResponse addTransition(@PathVariable UUID projectId, @Valid @RequestBody AddWorkflowTransitionRequest request) {
        return WorkflowTransitionResponse.from(workflowService.addTransition(projectId, request.fromStatus(), request.toStatus()));
    }
}

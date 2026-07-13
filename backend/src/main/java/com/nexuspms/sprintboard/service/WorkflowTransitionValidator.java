package com.nexuspms.sprintboard.service;

import com.nexuspms.common.exception.InvalidWorkflowTransitionException;
import com.nexuspms.common.exception.ResourceNotFoundException;
import com.nexuspms.sprintboard.domain.WorkflowStatus;
import com.nexuspms.sprintboard.repository.WorkflowStatusRepository;
import com.nexuspms.sprintboard.repository.WorkflowTransitionRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * LLD S6.2: checked AFTER RBAC (TRANSITION_STATUS), never before -- "is this
 * user allowed to transition issues at all" and "is this specific transition
 * legal in this project's workflow" are independent questions, and RBAC failing
 * should short-circuit before this ever runs.
 */
@Service
public class WorkflowTransitionValidator {

    private final WorkflowService workflowService;
    private final WorkflowStatusRepository workflowStatusRepository;
    private final WorkflowTransitionRepository workflowTransitionRepository;

    public WorkflowTransitionValidator(WorkflowService workflowService,
                                        WorkflowStatusRepository workflowStatusRepository,
                                        WorkflowTransitionRepository workflowTransitionRepository) {
        this.workflowService = workflowService;
        this.workflowStatusRepository = workflowStatusRepository;
        this.workflowTransitionRepository = workflowTransitionRepository;
    }

    /** Throws InvalidWorkflowTransitionException (-> 422, API Design S3) if illegal; returns silently if legal. */
    public void validate(UUID projectId, String issueKey, String fromStatus, String toStatus) {
        UUID definitionId = workflowService.getDefinition(projectId).getId();

        WorkflowStatus from = workflowStatusRepository.findByWorkflowDefinitionIdAndName(definitionId, fromStatus)
                .orElseThrow(() -> new ResourceNotFoundException("Unknown status: " + fromStatus));
        WorkflowStatus to = workflowStatusRepository.findByWorkflowDefinitionIdAndName(definitionId, toStatus)
                .orElseThrow(() -> new ResourceNotFoundException("Unknown status: " + toStatus));

        boolean legal = workflowTransitionRepository.findByFromStatusIdAndToStatusId(from.getId(), to.getId()).isPresent();
        if (!legal) {
            throw new InvalidWorkflowTransitionException(issueKey, fromStatus, toStatus);
        }
    }
}

package com.nexuspms.sprintboard.service;

import com.nexuspms.backlog.service.IssueService;
import com.nexuspms.common.exception.ResourceNotFoundException;
import com.nexuspms.governance.event.ProjectCreatedEvent;
import com.nexuspms.sprintboard.domain.WorkflowDefinition;
import com.nexuspms.sprintboard.domain.WorkflowStatus;
import com.nexuspms.sprintboard.domain.WorkflowTransition;
import com.nexuspms.sprintboard.repository.WorkflowDefinitionRepository;
import com.nexuspms.sprintboard.repository.WorkflowStatusRepository;
import com.nexuspms.sprintboard.repository.WorkflowTransitionRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * LLD S6.2: the Workflow Engine sub-component within Sprint & Board. PRD FR-30:
 * per-project customizable status workflow, seeded with a default graph on
 * project creation.
 *
 * The default initial status name ("To Do") MUST match
 * {@link IssueService#DEFAULT_INITIAL_STATUS} -- Backlog & Issues does not
 * depend on Sprint & Board (HLD S4), so new issues can't look up this
 * definition live; they default to the same literal by convention instead.
 */
@Service
public class WorkflowService {

    private final WorkflowDefinitionRepository workflowDefinitionRepository;
    private final WorkflowStatusRepository workflowStatusRepository;
    private final WorkflowTransitionRepository workflowTransitionRepository;

    public WorkflowService(WorkflowDefinitionRepository workflowDefinitionRepository,
                            WorkflowStatusRepository workflowStatusRepository,
                            WorkflowTransitionRepository workflowTransitionRepository) {
        this.workflowDefinitionRepository = workflowDefinitionRepository;
        this.workflowStatusRepository = workflowStatusRepository;
        this.workflowTransitionRepository = workflowTransitionRepository;
    }

    /** Reacts to Governance & RBAC's ProjectCreatedEvent (LLD S9) -- Sprint & Board depends on Governance, so consuming its event is the allowed direction. */
    @EventListener
    @Transactional
    public void onProjectCreated(ProjectCreatedEvent event) {
        seedDefaultWorkflow(event.projectId());
    }

    @Transactional
    public WorkflowDefinition seedDefaultWorkflow(UUID projectId) {
        WorkflowDefinition definition = workflowDefinitionRepository.save(new WorkflowDefinition(projectId));

        WorkflowStatus toDo = workflowStatusRepository.save(new WorkflowStatus(definition.getId(), IssueService.DEFAULT_INITIAL_STATUS, 0, true, false));
        WorkflowStatus inProgress = workflowStatusRepository.save(new WorkflowStatus(definition.getId(), "In Progress", 1, false, false));
        WorkflowStatus inReview = workflowStatusRepository.save(new WorkflowStatus(definition.getId(), "In Review", 2, false, false));
        WorkflowStatus done = workflowStatusRepository.save(new WorkflowStatus(definition.getId(), "Done", 3, false, true));

        workflowTransitionRepository.save(new WorkflowTransition(definition.getId(), toDo.getId(), inProgress.getId()));
        workflowTransitionRepository.save(new WorkflowTransition(definition.getId(), inProgress.getId(), inReview.getId()));
        workflowTransitionRepository.save(new WorkflowTransition(definition.getId(), inReview.getId(), inProgress.getId()));
        workflowTransitionRepository.save(new WorkflowTransition(definition.getId(), inReview.getId(), done.getId()));
        workflowTransitionRepository.save(new WorkflowTransition(definition.getId(), done.getId(), inProgress.getId()));

        return definition;
    }

    public WorkflowDefinition getDefinition(UUID projectId) {
        return workflowDefinitionRepository.findByProjectId(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("No workflow defined for project " + projectId));
    }

    public List<WorkflowStatus> listStatuses(UUID projectId) {
        return workflowStatusRepository.findByWorkflowDefinitionIdOrderByDisplayOrder(getDefinition(projectId).getId());
    }

    public List<WorkflowTransition> listTransitions(UUID projectId) {
        return workflowTransitionRepository.findByWorkflowDefinitionId(getDefinition(projectId).getId());
    }

    public List<String> terminalStatusNames(UUID projectId) {
        return listStatuses(projectId).stream().filter(WorkflowStatus::isTerminal).map(WorkflowStatus::getName).toList();
    }

    /** PRD FR-30: add a status to the project's workflow. */
    @Transactional
    public WorkflowStatus addStatus(UUID projectId, String name, int displayOrder, boolean isInitial, boolean isTerminal) {
        return workflowStatusRepository.save(new WorkflowStatus(getDefinition(projectId).getId(), name, displayOrder, isInitial, isTerminal));
    }

    /** PRD FR-30: define an allowed transition between two of the project's statuses. */
    @Transactional
    public WorkflowTransition addTransition(UUID projectId, String fromStatusName, String toStatusName) {
        UUID definitionId = getDefinition(projectId).getId();
        WorkflowStatus from = workflowStatusRepository.findByWorkflowDefinitionIdAndName(definitionId, fromStatusName)
                .orElseThrow(() -> new ResourceNotFoundException("Unknown status: " + fromStatusName));
        WorkflowStatus to = workflowStatusRepository.findByWorkflowDefinitionIdAndName(definitionId, toStatusName)
                .orElseThrow(() -> new ResourceNotFoundException("Unknown status: " + toStatusName));
        return workflowTransitionRepository.save(new WorkflowTransition(definitionId, from.getId(), to.getId()));
    }
}

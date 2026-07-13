package com.nexuspms.sprintboard.service;

import com.nexuspms.common.exception.InvalidWorkflowTransitionException;
import com.nexuspms.sprintboard.domain.WorkflowDefinition;
import com.nexuspms.sprintboard.domain.WorkflowStatus;
import com.nexuspms.sprintboard.repository.WorkflowStatusRepository;
import com.nexuspms.sprintboard.repository.WorkflowTransitionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/** LLD S6.2: checked after RBAC, never before -- these tests cover the workflow-graph decision itself in isolation. */
@ExtendWith(MockitoExtension.class)
class WorkflowTransitionValidatorTest {

    @Mock
    WorkflowService workflowService;
    @Mock
    WorkflowStatusRepository workflowStatusRepository;
    @Mock
    WorkflowTransitionRepository workflowTransitionRepository;

    private WorkflowTransitionValidator validator() {
        return new WorkflowTransitionValidator(workflowService, workflowStatusRepository, workflowTransitionRepository);
    }

    @Test
    void legalTransition_doesNotThrow() {
        UUID projectId = UUID.randomUUID();
        UUID definitionId = UUID.randomUUID();
        WorkflowStatus fromStatus = new WorkflowStatus(definitionId, "To Do", 0, true, false);
        WorkflowStatus toStatus = new WorkflowStatus(definitionId, "In Progress", 1, false, false);

        when(workflowService.getDefinition(projectId)).thenReturn(new WorkflowDefinition(projectId));
        when(workflowStatusRepository.findByWorkflowDefinitionIdAndName(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq("To Do")))
                .thenReturn(Optional.of(fromStatus));
        when(workflowStatusRepository.findByWorkflowDefinitionIdAndName(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq("In Progress")))
                .thenReturn(Optional.of(toStatus));
        when(workflowTransitionRepository.findByFromStatusIdAndToStatusId(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(Optional.of(new com.nexuspms.sprintboard.domain.WorkflowTransition(definitionId, UUID.randomUUID(), UUID.randomUUID())));

        assertThatCode(() -> validator().validate(projectId, "NEX-1", "To Do", "In Progress")).doesNotThrowAnyException();
    }

    @Test
    void illegalTransition_throwsInvalidWorkflowTransitionException() {
        UUID projectId = UUID.randomUUID();
        UUID definitionId = UUID.randomUUID();
        WorkflowStatus fromStatus = new WorkflowStatus(definitionId, "Done", 3, false, true);
        WorkflowStatus toStatus = new WorkflowStatus(definitionId, "To Do", 0, true, false);

        when(workflowService.getDefinition(projectId)).thenReturn(new WorkflowDefinition(projectId));
        when(workflowStatusRepository.findByWorkflowDefinitionIdAndName(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq("Done")))
                .thenReturn(Optional.of(fromStatus));
        when(workflowStatusRepository.findByWorkflowDefinitionIdAndName(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq("To Do")))
                .thenReturn(Optional.of(toStatus));
        when(workflowTransitionRepository.findByFromStatusIdAndToStatusId(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> validator().validate(projectId, "NEX-1", "Done", "To Do"))
                .isInstanceOf(InvalidWorkflowTransitionException.class);
    }
}

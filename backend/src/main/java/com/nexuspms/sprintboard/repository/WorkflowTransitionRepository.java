package com.nexuspms.sprintboard.repository;

import com.nexuspms.sprintboard.domain.WorkflowTransition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkflowTransitionRepository extends JpaRepository<WorkflowTransition, UUID> {

    List<WorkflowTransition> findByWorkflowDefinitionId(UUID workflowDefinitionId);

    Optional<WorkflowTransition> findByFromStatusIdAndToStatusId(UUID fromStatusId, UUID toStatusId);
}

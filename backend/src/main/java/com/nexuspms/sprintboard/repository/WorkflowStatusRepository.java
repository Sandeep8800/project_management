package com.nexuspms.sprintboard.repository;

import com.nexuspms.sprintboard.domain.WorkflowStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkflowStatusRepository extends JpaRepository<WorkflowStatus, UUID> {

    List<WorkflowStatus> findByWorkflowDefinitionIdOrderByDisplayOrder(UUID workflowDefinitionId);

    Optional<WorkflowStatus> findByWorkflowDefinitionIdAndName(UUID workflowDefinitionId, String name);
}

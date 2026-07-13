package com.nexuspms.sprintboard.repository;

import com.nexuspms.sprintboard.domain.WorkflowDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WorkflowDefinitionRepository extends JpaRepository<WorkflowDefinition, UUID> {

    Optional<WorkflowDefinition> findByProjectId(UUID projectId);
}

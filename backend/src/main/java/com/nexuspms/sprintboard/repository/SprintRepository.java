package com.nexuspms.sprintboard.repository;

import com.nexuspms.sprintboard.domain.Sprint;
import com.nexuspms.sprintboard.domain.SprintStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SprintRepository extends JpaRepository<Sprint, UUID> {

    List<Sprint> findByProjectId(UUID projectId);

    List<Sprint> findByProjectIdAndStatus(UUID projectId, SprintStatus status);

    Optional<Sprint> findFirstByProjectIdAndStatus(UUID projectId, SprintStatus status);
}

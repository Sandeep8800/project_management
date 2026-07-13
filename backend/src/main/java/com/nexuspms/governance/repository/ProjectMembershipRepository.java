package com.nexuspms.governance.repository;

import com.nexuspms.governance.domain.ProjectMembership;
import com.nexuspms.governance.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectMembershipRepository extends JpaRepository<ProjectMembership, UUID> {

    Optional<ProjectMembership> findByUserIdAndProjectId(UUID userId, UUID projectId);

    List<ProjectMembership> findByProjectId(UUID projectId);

    List<ProjectMembership> findByUserId(UUID userId);

    boolean existsByUserIdAndRole(UUID userId, Role role);

    long countByProjectId(UUID projectId);
}

package com.nexuspms.governance.repository;

import com.nexuspms.governance.domain.Project;
import com.nexuspms.governance.domain.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    boolean existsByProjectKey(String projectKey);

    Optional<Project> findByProjectKey(String projectKey);

    @Query("""
            select p from Project p
            where (:status is null or p.status = :status)
            order by p.createdAt desc
            """)
    Page<Project> search(@Param("status") ProjectStatus status, Pageable pageable);

    long countByStatus(ProjectStatus status);
}

package com.nexuspms.governance.service;

import com.nexuspms.common.event.DomainEventPublisher;
import com.nexuspms.common.exception.GovernanceSafeguardException;
import com.nexuspms.common.exception.ResourceNotFoundException;
import com.nexuspms.governance.domain.Project;
import com.nexuspms.governance.domain.ProjectMethodology;
import com.nexuspms.governance.domain.ProjectStatus;
import com.nexuspms.governance.event.ProjectArchivedEvent;
import com.nexuspms.governance.event.ProjectCreatedEvent;
import com.nexuspms.governance.event.ProjectDeletedEvent;
import com.nexuspms.governance.repository.ProjectRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/** PRD S3.1.2 / LLD S4.3: project lifecycle, admin-only (enforced at the controller layer via hasRole('ADMIN')). */
@Service
public class ProjectAdminService {

    private final ProjectRepository projectRepository;
    private final AuditService auditService;
    private final DomainEventPublisher eventPublisher;

    public ProjectAdminService(ProjectRepository projectRepository, AuditService auditService, DomainEventPublisher eventPublisher) {
        this.projectRepository = projectRepository;
        this.auditService = auditService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Project create(UUID actorId, String projectKey, String name, String description,
                           ProjectMethodology methodology, LocalDate startDate, LocalDate targetReleaseDate) {
        if (projectRepository.existsByProjectKey(projectKey)) {
            throw new GovernanceSafeguardException("Project key '" + projectKey + "' is already in use.");
        }
        Project project = new Project(projectKey, name, description, methodology, startDate, targetReleaseDate);
        projectRepository.save(project);
        auditService.record(actorId, "PROJECT_CREATED", "PROJECT", project.getId(), Map.of("projectKey", projectKey));
        eventPublisher.publish(new ProjectCreatedEvent(project.getId()));
        return project;
    }

    public Page<Project> search(ProjectStatus status, Pageable pageable) {
        return projectRepository.search(status, pageable);
    }

    public Project get(UUID projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project " + projectId + " not found."));
    }

    @Transactional
    public void archive(UUID actorId, UUID projectId) {
        Project project = get(projectId);
        project.archive();
        auditService.record(actorId, "PROJECT_ARCHIVED", "PROJECT", projectId, Map.of());
        eventPublisher.publish(new ProjectArchivedEvent(projectId));
    }

    /** PRD FR-8: throws GovernanceSafeguardException via Project.markDeleted() unless already archived. */
    @Transactional
    public void delete(UUID actorId, UUID projectId) {
        Project project = get(projectId);
        project.markDeleted();
        auditService.record(actorId, "PROJECT_DELETED", "PROJECT", projectId, Map.of());
        eventPublisher.publish(new ProjectDeletedEvent(projectId));
    }
}

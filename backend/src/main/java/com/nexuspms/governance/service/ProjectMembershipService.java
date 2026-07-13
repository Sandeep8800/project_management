package com.nexuspms.governance.service;

import com.nexuspms.common.event.DomainEventPublisher;
import com.nexuspms.common.exception.GovernanceSafeguardException;
import com.nexuspms.common.exception.ResourceNotFoundException;
import com.nexuspms.governance.domain.ProjectMembership;
import com.nexuspms.governance.domain.Role;
import com.nexuspms.governance.event.ProjectMembershipChangedEvent;
import com.nexuspms.governance.repository.ProjectMembershipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** PRD S3.1.3: user-to-project assignment and role mapping -- admin-only (Database Design S10 design note). */
@Service
public class ProjectMembershipService {

    private final ProjectMembershipRepository projectMembershipRepository;
    private final AuditService auditService;
    private final DomainEventPublisher eventPublisher;

    public ProjectMembershipService(ProjectMembershipRepository projectMembershipRepository,
                                     AuditService auditService,
                                     DomainEventPublisher eventPublisher) {
        this.projectMembershipRepository = projectMembershipRepository;
        this.auditService = auditService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public ProjectMembership assign(UUID actorId, UUID projectId, UUID userId, Role role) {
        projectMembershipRepository.findByUserIdAndProjectId(userId, projectId).ifPresent(m -> {
            throw new GovernanceSafeguardException(
                    "User is already assigned to this project -- use the role-change endpoint instead.");
        });
        ProjectMembership membership = new ProjectMembership(userId, projectId, role, actorId);
        projectMembershipRepository.save(membership);
        auditService.record(actorId, "ROLE_ASSIGNED", "PROJECT_MEMBERSHIP", membership.getId(),
                Map.of("userId", userId.toString(), "projectId", projectId.toString(), "role", role.name()));
        eventPublisher.publish(new ProjectMembershipChangedEvent(userId, projectId));
        return membership;
    }

    @Transactional
    public ProjectMembership changeRole(UUID actorId, UUID projectId, UUID userId, Role newRole) {
        ProjectMembership membership = get(projectId, userId);
        Role oldRole = membership.getRole();
        membership.changeRole(newRole);
        auditService.record(actorId, "ROLE_CHANGED", "PROJECT_MEMBERSHIP", membership.getId(),
                Map.of("oldRole", oldRole.name(), "newRole", newRole.name()));
        eventPublisher.publish(new ProjectMembershipChangedEvent(userId, projectId));
        return membership;
    }

    @Transactional
    public void remove(UUID actorId, UUID projectId, UUID userId) {
        ProjectMembership membership = get(projectId, userId);
        projectMembershipRepository.delete(membership);
        auditService.record(actorId, "MEMBERSHIP_REMOVED", "PROJECT_MEMBERSHIP", membership.getId(),
                Map.of("userId", userId.toString(), "projectId", projectId.toString()));
        eventPublisher.publish(new ProjectMembershipChangedEvent(userId, projectId));
    }

    public List<ProjectMembership> listByProject(UUID projectId) {
        return projectMembershipRepository.findByProjectId(projectId);
    }

    public List<ProjectMembership> listByUser(UUID userId) {
        return projectMembershipRepository.findByUserId(userId);
    }

    private ProjectMembership get(UUID projectId, UUID userId) {
        return projectMembershipRepository.findByUserIdAndProjectId(userId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("No membership found for this user on this project."));
    }
}

package com.nexuspms.governance.service;

import com.nexuspms.governance.domain.Permission;
import com.nexuspms.governance.domain.ProjectMembership;
import com.nexuspms.governance.repository.ProjectMembershipRepository;
import com.nexuspms.governance.repository.RolePermissionRepository;
import com.nexuspms.identity.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * LLD S4.2: the sole authority every controller relies on to answer "can this
 * caller do this action on this project." AuthorizationAspect (governance/security)
 * is the single call site that invokes this -- no module implements its own ad-hoc
 * check.
 *
 * Note: HLD S5 specifies a Redis-backed cache for this resolution, invalidated on
 * ProjectMembershipChanged. That cache layer is an infra optimization not wired in
 * this pass -- every call here queries the database directly, which is correct but
 * not yet the target-scale-sized implementation.
 */
@Service
public class PermissionResolver {

    private final ProjectMembershipRepository projectMembershipRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRepository userRepository;

    public PermissionResolver(ProjectMembershipRepository projectMembershipRepository,
                               RolePermissionRepository rolePermissionRepository,
                               UserRepository userRepository) {
        this.projectMembershipRepository = projectMembershipRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.userRepository = userRepository;
    }

    /** HLD S1: Admin has platform-wide scope, independent of any project_membership row (see V8 migration note). */
    public boolean isPlatformAdmin(UUID userId) {
        return userRepository.findById(userId).map(com.nexuspms.identity.domain.User::isPlatformAdmin).orElse(false);
    }

    public boolean hasMembership(UUID userId, UUID projectId) {
        return isPlatformAdmin(userId) || findMembership(userId, projectId).isPresent();
    }

    public Set<Permission> resolve(UUID userId, UUID projectId) {
        if (isPlatformAdmin(userId)) {
            return EnumSet.allOf(Permission.class);
        }
        return findMembership(userId, projectId)
                .map(m -> {
                    Set<Permission> permissions = EnumSet.noneOf(Permission.class);
                    permissions.addAll(rolePermissionRepository.findPermissionsByRole(m.getRole()));
                    return permissions;
                })
                .orElse(EnumSet.noneOf(Permission.class));
    }

    public boolean hasPermission(UUID userId, UUID projectId, Permission permission) {
        return resolve(userId, projectId).contains(permission);
    }

    private Optional<ProjectMembership> findMembership(UUID userId, UUID projectId) {
        return projectMembershipRepository.findByUserIdAndProjectId(userId, projectId);
    }
}

package com.nexuspms.governance.service;

import com.nexuspms.governance.domain.Permission;
import com.nexuspms.governance.domain.ProjectMembership;
import com.nexuspms.governance.domain.Role;
import com.nexuspms.governance.repository.ProjectMembershipRepository;
import com.nexuspms.governance.repository.RolePermissionRepository;
import com.nexuspms.identity.domain.User;
import com.nexuspms.identity.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * LLD S14: PermissionResolver carries the most business-rule risk in the system --
 * every mutating endpoint's authorization decision runs through it.
 */
@ExtendWith(MockitoExtension.class)
class PermissionResolverTest {

    @Mock
    ProjectMembershipRepository projectMembershipRepository;
    @Mock
    RolePermissionRepository rolePermissionRepository;
    @Mock
    UserRepository userRepository;

    private PermissionResolver resolver() {
        return new PermissionResolver(projectMembershipRepository, rolePermissionRepository, userRepository);
    }

    @Test
    void platformAdmin_getsFullPermissionSet_regardlessOfProjectMembership() {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        User admin = new User("Admin", "admin@example.com", null, null, "ADMIN");
        admin.grantPlatformAdmin();
        when(userRepository.findById(userId)).thenReturn(Optional.of(admin));

        Set<Permission> permissions = resolver().resolve(userId, projectId);

        assertThat(permissions).containsExactlyInAnyOrder(Permission.values());
    }

    @Test
    void nonAdminMember_getsExactlyTheirRolesPermissions() {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        User developer = new User("Dev", "dev@example.com", null, null, "DEVELOPER");
        when(userRepository.findById(userId)).thenReturn(Optional.of(developer));
        when(projectMembershipRepository.findByUserIdAndProjectId(userId, projectId))
                .thenReturn(Optional.of(new ProjectMembership(userId, projectId, Role.DEVELOPER, UUID.randomUUID())));
        when(rolePermissionRepository.findPermissionsByRole(Role.DEVELOPER))
                .thenReturn(List.of(Permission.CREATE_ISSUE, Permission.EDIT_ISSUE, Permission.TRANSITION_STATUS, Permission.VIEW_REPORTS));

        Set<Permission> permissions = resolver().resolve(userId, projectId);

        assertThat(permissions).containsExactlyInAnyOrder(
                Permission.CREATE_ISSUE, Permission.EDIT_ISSUE, Permission.TRANSITION_STATUS, Permission.VIEW_REPORTS);
        assertThat(permissions).doesNotContain(Permission.DELETE_ISSUE, Permission.MANAGE_PROJECT_USERS);
    }

    @Test
    void nonMember_getsNoPermissions_notAnException() {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        User user = new User("Outsider", "outsider@example.com", null, null, "DEVELOPER");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(projectMembershipRepository.findByUserIdAndProjectId(userId, projectId)).thenReturn(Optional.empty());

        Set<Permission> permissions = resolver().resolve(userId, projectId);

        assertThat(permissions).isEmpty();
    }

    @Test
    void hasMembership_trueForPlatformAdmin_evenWithNoMembershipRow() {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        User admin = new User("Admin", "admin@example.com", null, null, "ADMIN");
        admin.grantPlatformAdmin();
        when(userRepository.findById(userId)).thenReturn(Optional.of(admin));

        assertThat(resolver().hasMembership(userId, projectId)).isTrue();
    }

    @Test
    void hasMembership_falseForUnknownUser_doesNotThrow() {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        when(projectMembershipRepository.findByUserIdAndProjectId(userId, projectId)).thenReturn(Optional.empty());

        assertThat(resolver().hasMembership(userId, projectId)).isFalse();
    }
}

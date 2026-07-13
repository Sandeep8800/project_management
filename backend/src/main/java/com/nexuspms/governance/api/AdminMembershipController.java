package com.nexuspms.governance.api;

import com.nexuspms.common.security.CurrentUser;
import com.nexuspms.governance.api.dto.AssignMembershipRequest;
import com.nexuspms.governance.api.dto.ChangeRoleRequest;
import com.nexuspms.governance.api.dto.MembershipResponse;
import com.nexuspms.governance.service.ProjectMembershipService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** API Design S5: PRD FR-10/FR-11/FR-14, platform-Admin-only. */
@RestController
@RequestMapping("/admin/projects/{projectId}/memberships")
@PreAuthorize("hasRole('ADMIN')")
public class AdminMembershipController {

    private final ProjectMembershipService projectMembershipService;
    private final CurrentUser currentUser;

    public AdminMembershipController(ProjectMembershipService projectMembershipService, CurrentUser currentUser) {
        this.projectMembershipService = projectMembershipService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<MembershipResponse> list(@PathVariable UUID projectId) {
        return projectMembershipService.listByProject(projectId).stream().map(MembershipResponse::from).toList();
    }

    @PostMapping
    public MembershipResponse assign(@PathVariable UUID projectId, @Valid @RequestBody AssignMembershipRequest request) {
        return MembershipResponse.from(
                projectMembershipService.assign(currentUser.requireUserId(), projectId, request.userId(), request.role()));
    }

    @PatchMapping("/{userId}")
    public MembershipResponse changeRole(@PathVariable UUID projectId, @PathVariable UUID userId,
                                          @Valid @RequestBody ChangeRoleRequest request) {
        return MembershipResponse.from(
                projectMembershipService.changeRole(currentUser.requireUserId(), projectId, userId, request.role()));
    }

    @DeleteMapping("/{userId}")
    public void remove(@PathVariable UUID projectId, @PathVariable UUID userId) {
        projectMembershipService.remove(currentUser.requireUserId(), projectId, userId);
    }
}

package com.nexuspms.governance.api;

import com.nexuspms.common.security.CurrentUser;
import com.nexuspms.governance.api.dto.MeProjectResponse;
import com.nexuspms.governance.domain.Project;
import com.nexuspms.governance.domain.ProjectMembership;
import com.nexuspms.governance.repository.ProjectRepository;
import com.nexuspms.governance.service.ProjectMembershipService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * API Design S4 GET /me/projects: the caller's own project_memberships, distinct
 * from the admin-wide GET /admin/projects. No project-level permission required
 * beyond authentication -- a user's own membership list can't leak anything they
 * don't already have access to (added to API Design 2026-07-13 per the UI Design
 * S3 gap).
 */
@RestController
@RequestMapping("/me/projects")
public class MeProjectsController {

    private final ProjectMembershipService projectMembershipService;
    private final ProjectRepository projectRepository;
    private final CurrentUser currentUser;

    public MeProjectsController(ProjectMembershipService projectMembershipService,
                                 ProjectRepository projectRepository,
                                 CurrentUser currentUser) {
        this.projectMembershipService = projectMembershipService;
        this.projectRepository = projectRepository;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<MeProjectResponse> myProjects() {
        UUID userId = currentUser.requireUserId();
        List<ProjectMembership> memberships = projectMembershipService.listByUser(userId);

        Map<UUID, Project> projectsById = projectRepository.findAllById(
                memberships.stream().map(ProjectMembership::getProjectId).toList()
        ).stream().collect(java.util.stream.Collectors.toMap(Project::getId, p -> p));

        return memberships.stream()
                .map(m -> {
                    Project project = projectsById.get(m.getProjectId());
                    return new MeProjectResponse(m.getProjectId(), project.getProjectKey(), project.getName(), m.getRole().name());
                })
                .toList();
    }
}

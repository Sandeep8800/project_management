package com.nexuspms.governance.api;

import com.nexuspms.common.security.CurrentUser;
import com.nexuspms.common.web.PageResponse;
import com.nexuspms.governance.api.dto.CreateProjectRequest;
import com.nexuspms.governance.api.dto.ProjectResponse;
import com.nexuspms.governance.domain.Project;
import com.nexuspms.governance.domain.ProjectStatus;
import com.nexuspms.governance.service.ProjectAdminService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** API Design S5: PRD FR-6..9, platform-Admin-only (governance action, not a project-scoped permission check). */
@RestController
@RequestMapping("/admin/projects")
@PreAuthorize("hasRole('ADMIN')")
public class AdminProjectController {

    private final ProjectAdminService projectAdminService;
    private final CurrentUser currentUser;

    public AdminProjectController(ProjectAdminService projectAdminService, CurrentUser currentUser) {
        this.projectAdminService = projectAdminService;
        this.currentUser = currentUser;
    }

    @PostMapping
    public ProjectResponse create(@Valid @RequestBody CreateProjectRequest request) {
        Project project = projectAdminService.create(
                currentUser.requireUserId(), request.projectKey(), request.name(), request.description(),
                request.methodology(), request.startDate(), request.targetReleaseDate());
        return ProjectResponse.from(project);
    }

    @GetMapping
    public PageResponse<ProjectResponse> list(@RequestParam(required = false) ProjectStatus status,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        Page<Project> result = projectAdminService.search(status, PageRequest.of(page, Math.min(size, 100)));
        return PageResponse.of(result, ProjectResponse::from);
    }

    @GetMapping("/{projectId}")
    public ProjectResponse get(@PathVariable UUID projectId) {
        return ProjectResponse.from(projectAdminService.get(projectId));
    }

    @PostMapping("/{projectId}/archive")
    public void archive(@PathVariable UUID projectId) {
        projectAdminService.archive(currentUser.requireUserId(), projectId);
    }

    @DeleteMapping("/{projectId}")
    public void delete(@PathVariable UUID projectId) {
        projectAdminService.delete(currentUser.requireUserId(), projectId);
    }
}

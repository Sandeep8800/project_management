package com.nexuspms.governance.api;

import com.nexuspms.governance.api.dto.DashboardResponse;
import com.nexuspms.governance.domain.ProjectStatus;
import com.nexuspms.governance.repository.ProjectRepository;
import com.nexuspms.identity.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** API Design S5 / PRD FR-16: consolidated projects/users/role-assignment view. */
@RestController
@RequestMapping("/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public AdminDashboardController(ProjectRepository projectRepository, UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public DashboardResponse dashboard() {
        return new DashboardResponse(
                projectRepository.countByStatus(ProjectStatus.ACTIVE),
                projectRepository.countByStatus(ProjectStatus.ARCHIVED),
                userRepository.count());
    }
}

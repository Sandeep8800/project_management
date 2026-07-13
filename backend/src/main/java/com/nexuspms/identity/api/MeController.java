package com.nexuspms.identity.api;

import com.nexuspms.identity.api.dto.UserResponse;
import com.nexuspms.identity.service.UserAdminService;
import com.nexuspms.common.security.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** API Design S4: GET /me -- caller's own profile. GET /me/projects lives in the governance module (MeProjectsController), since project_memberships is Governance & RBAC's data. */
@RestController
@RequestMapping("/me")
public class MeController {

    private final UserAdminService userAdminService;
    private final CurrentUser currentUser;

    public MeController(UserAdminService userAdminService, CurrentUser currentUser) {
        this.userAdminService = userAdminService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public UserResponse me() {
        return UserResponse.from(userAdminService.get(currentUser.requireUserId()));
    }
}

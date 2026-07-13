package com.nexuspms.identity.api;

import com.nexuspms.common.security.CurrentUser;
import com.nexuspms.common.web.PageResponse;
import com.nexuspms.identity.api.dto.CreateUserRequest;
import com.nexuspms.identity.api.dto.ResetPasswordRequest;
import com.nexuspms.identity.api.dto.UpdateUserRequest;
import com.nexuspms.identity.api.dto.UserResponse;
import com.nexuspms.identity.domain.User;
import com.nexuspms.identity.domain.UserStatus;
import com.nexuspms.identity.service.UserAdminService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * API Design S5: all admin/* endpoints require platform-wide Admin -- a
 * project-scoped permission check doesn't apply here since these are governance
 * actions (PRD S3.1), not project-content actions.
 */
@RestController
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserAdminService userAdminService;
    private final CurrentUser currentUser;

    public AdminUserController(UserAdminService userAdminService, CurrentUser currentUser) {
        this.userAdminService = userAdminService;
        this.currentUser = currentUser;
    }

    @PostMapping
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        User user = userAdminService.createUser(
                currentUser.requireUserId(), request.name(), request.email(), request.employeeId(),
                request.department(), request.defaultRole(), request.initialPassword());
        return UserResponse.from(user);
    }

    @GetMapping
    public PageResponse<UserResponse> list(@RequestParam(required = false) UserStatus status,
                                            @RequestParam(required = false) String department,
                                            @RequestParam(required = false) String q,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        Page<User> result = userAdminService.search(status, department, q, PageRequest.of(page, Math.min(size, 100)));
        return PageResponse.of(result, UserResponse::from);
    }

    @GetMapping("/{userId}")
    public UserResponse get(@PathVariable UUID userId) {
        return UserResponse.from(userAdminService.get(userId));
    }

    @PatchMapping("/{userId}")
    public UserResponse update(@PathVariable UUID userId, @RequestBody UpdateUserRequest request) {
        return UserResponse.from(userAdminService.update(userId, request.name(), request.department(), request.defaultRole()));
    }

    @PostMapping("/{userId}/deactivate")
    public void deactivate(@PathVariable UUID userId) {
        userAdminService.deactivate(currentUser.requireUserId(), userId);
    }

    @PostMapping("/{userId}/reactivate")
    public void reactivate(@PathVariable UUID userId) {
        userAdminService.reactivate(currentUser.requireUserId(), userId);
    }

    @PostMapping("/{userId}/reset-password")
    public void resetPassword(@PathVariable UUID userId, @Valid @RequestBody ResetPasswordRequest request) {
        userAdminService.resetPassword(userId, request.newPassword());
    }

    @PostMapping("/{userId}/grant-admin")
    public void grantAdmin(@PathVariable UUID userId) {
        userAdminService.grantPlatformAdmin(currentUser.requireUserId(), userId);
    }

    @PostMapping("/{userId}/revoke-admin")
    public void revokeAdmin(@PathVariable UUID userId) {
        userAdminService.revokePlatformAdmin(currentUser.requireUserId(), userId);
    }
}

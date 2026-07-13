package com.nexuspms.notifications.api;

import com.nexuspms.common.security.CurrentUser;
import com.nexuspms.common.web.PageResponse;
import com.nexuspms.identity.domain.User;
import com.nexuspms.identity.service.UserAdminService;
import com.nexuspms.notifications.api.dto.NotificationPreferencesResponse;
import com.nexuspms.notifications.api.dto.NotificationResponse;
import com.nexuspms.notifications.api.dto.UpdateNotificationPreferencesRequest;
import com.nexuspms.notifications.service.NotificationService;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** API Design S9. */
@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserAdminService userAdminService;
    private final CurrentUser currentUser;

    public NotificationController(NotificationService notificationService, UserAdminService userAdminService, CurrentUser currentUser) {
        this.notificationService = notificationService;
        this.userAdminService = userAdminService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public PageResponse<NotificationResponse> list(@RequestParam(defaultValue = "false") boolean unreadOnly,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "20") int size) {
        return PageResponse.of(
                notificationService.list(currentUser.requireUserId(), unreadOnly, PageRequest.of(page, Math.min(size, 100))),
                NotificationResponse::from);
    }

    @PostMapping("/{id}/read")
    public void markRead(@PathVariable UUID id) {
        notificationService.markRead(currentUser.requireUserId(), id);
    }

    @PostMapping("/read-all")
    public void markAllRead() {
        notificationService.markAllRead(currentUser.requireUserId());
    }

    @GetMapping("/preferences")
    public NotificationPreferencesResponse preferences() {
        User user = userAdminService.get(currentUser.requireUserId());
        return new NotificationPreferencesResponse(true, user.isEmailNotificationsEnabled());
    }

    @PatchMapping("/preferences")
    public NotificationPreferencesResponse updatePreferences(@RequestBody UpdateNotificationPreferencesRequest request) {
        userAdminService.setEmailNotificationsEnabled(currentUser.requireUserId(), request.emailEnabled());
        return new NotificationPreferencesResponse(true, request.emailEnabled());
    }
}

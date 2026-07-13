package com.nexuspms.realtime;

import com.nexuspms.backlog.event.IssueAssignedEvent;
import com.nexuspms.backlog.event.UserMentionedEvent;
import com.nexuspms.sprintboard.event.SprintCompletedEvent;
import com.nexuspms.sprintboard.event.SprintStartedEvent;
import com.nexuspms.governance.service.ProjectMembershipService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * HLD S8.1/S8.4: an independent consumer of the same events
 * NotificationDispatchService reacts to -- not called by it. Pushes to
 * /user/{id}/queue/notifications via Spring's user-destination resolution,
 * which maps to the Principal set on the STOMP session at CONNECT time
 * (StompAuthChannelInterceptor).
 */
@Component
public class NotificationPushService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ProjectMembershipService projectMembershipService;

    public NotificationPushService(SimpMessagingTemplate messagingTemplate, ProjectMembershipService projectMembershipService) {
        this.messagingTemplate = messagingTemplate;
        this.projectMembershipService = projectMembershipService;
    }

    @EventListener
    public void onIssueAssigned(IssueAssignedEvent event) {
        pushTo(event.assigneeId(), "ISSUE_ASSIGNED", Map.of("issueId", event.issueId().toString()));
    }

    @EventListener
    public void onUserMentioned(UserMentionedEvent event) {
        pushTo(event.mentionedUserId(), "USER_MENTIONED", Map.of("issueId", event.issueId().toString(), "commentId", event.commentId().toString()));
    }

    @EventListener
    public void onSprintStarted(SprintStartedEvent event) {
        projectMembershipService.listByProject(event.projectId())
                .forEach(m -> pushTo(m.getUserId(), "SPRINT_STARTED", Map.of("sprintId", event.sprintId().toString())));
    }

    @EventListener
    public void onSprintCompleted(SprintCompletedEvent event) {
        projectMembershipService.listByProject(event.projectId())
                .forEach(m -> pushTo(m.getUserId(), "SPRINT_COMPLETED", Map.of("sprintId", event.sprintId().toString())));
    }

    private void pushTo(UUID userId, String type, Map<String, Object> payload) {
        messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/notifications", Map.of("type", type, "data", payload));
    }
}

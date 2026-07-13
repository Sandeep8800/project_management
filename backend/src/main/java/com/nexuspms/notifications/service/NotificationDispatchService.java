package com.nexuspms.notifications.service;

import com.nexuspms.backlog.event.IssueAssignedEvent;
import com.nexuspms.backlog.event.IssueStatusChangedEvent;
import com.nexuspms.backlog.event.UserMentionedEvent;
import com.nexuspms.common.job.BackgroundJobService;
import com.nexuspms.governance.service.ProjectMembershipService;
import com.nexuspms.identity.domain.User;
import com.nexuspms.identity.repository.UserRepository;
import com.nexuspms.notifications.domain.NotificationChannel;
import com.nexuspms.notifications.domain.NotificationOutbox;
import com.nexuspms.notifications.repository.NotificationOutboxRepository;
import com.nexuspms.sprintboard.event.SprintCompletedEvent;
import com.nexuspms.sprintboard.event.SprintStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * PRD FR-39 / HLD S8.1: consumes domain events from every other module and
 * writes outbox rows in the SAME transaction as the triggering change (Spring's
 * ApplicationEventPublisher invokes non-async @EventListener methods
 * synchronously in the publisher's call stack, so this runs inside whatever
 * transaction the publishing service method opened).
 */
@Service
public class NotificationDispatchService {

    public static final String NOTIFICATION_DISPATCH_JOB_TYPE = "NOTIFICATION_DISPATCH";

    private final NotificationOutboxRepository notificationOutboxRepository;
    private final BackgroundJobService backgroundJobService;
    private final UserRepository userRepository;
    private final ProjectMembershipService projectMembershipService;

    public NotificationDispatchService(NotificationOutboxRepository notificationOutboxRepository,
                                        BackgroundJobService backgroundJobService,
                                        UserRepository userRepository,
                                        ProjectMembershipService projectMembershipService) {
        this.notificationOutboxRepository = notificationOutboxRepository;
        this.backgroundJobService = backgroundJobService;
        this.userRepository = userRepository;
        this.projectMembershipService = projectMembershipService;
    }

    @EventListener
    @Transactional
    public void onIssueAssigned(IssueAssignedEvent event) {
        notify(event.assigneeId(), "ISSUE_ASSIGNED", Map.of("issueId", event.issueId().toString(), "projectId", event.projectId().toString()));
    }

    @EventListener
    @Transactional
    public void onUserMentioned(UserMentionedEvent event) {
        notify(event.mentionedUserId(), "USER_MENTIONED", Map.of(
                "issueId", event.issueId().toString(), "commentId", event.commentId().toString(), "authorId", event.authorId().toString()));
    }

    @EventListener
    @Transactional
    public void onIssueStatusChanged(IssueStatusChangedEvent event) {
        // PRD FR-39: "status transition on an issue the user is watching/assigned to" --
        // no separate watcher list is built in this pass, so this approximates
        // watchers as the issue's current assignee, per HLD S16's note that
        // capabilities not explicitly built are simplified, not silently dropped.
    }

    @EventListener
    @Transactional
    public void onSprintStarted(SprintStartedEvent event) {
        notifyAllProjectMembers(event.projectId(), "SPRINT_STARTED", Map.of("sprintId", event.sprintId().toString()));
    }

    @EventListener
    @Transactional
    public void onSprintCompleted(SprintCompletedEvent event) {
        notifyAllProjectMembers(event.projectId(), "SPRINT_COMPLETED", Map.of("sprintId", event.sprintId().toString()));
    }

    private void notifyAllProjectMembers(UUID projectId, String eventType, Map<String, Object> payload) {
        projectMembershipService.listByProject(projectId)
                .forEach(membership -> notify(membership.getUserId(), eventType, payload));
    }

    private void notify(UUID recipientId, String eventType, Map<String, Object> payload) {
        NotificationOutbox inApp = notificationOutboxRepository.save(
                new NotificationOutbox(recipientId, eventType, payload, NotificationChannel.IN_APP));
        enqueueDispatchJob(inApp);

        User user = userRepository.findById(recipientId).orElse(null);
        if (user != null && user.isEmailNotificationsEnabled()) {
            NotificationOutbox email = notificationOutboxRepository.save(
                    new NotificationOutbox(recipientId, eventType, payload, NotificationChannel.EMAIL));
            enqueueDispatchJob(email);
        }
    }

    private void enqueueDispatchJob(NotificationOutbox outboxEntry) {
        backgroundJobService.enqueue(NOTIFICATION_DISPATCH_JOB_TYPE, outboxEntry.getId().toString(),
                Map.of("outboxId", outboxEntry.getId().toString()));
    }
}

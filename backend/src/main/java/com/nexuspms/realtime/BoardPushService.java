package com.nexuspms.realtime;

import com.nexuspms.backlog.event.IssueAssignedEvent;
import com.nexuspms.backlog.event.IssueCreatedEvent;
import com.nexuspms.backlog.event.IssueStatusChangedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * HLD S8.4: board-state-change push. Both board types are notified regardless
 * of which triggered the change -- boards are views over one shared backlog
 * (HLD S4), so a card moved on one view is the same issue reflected on the
 * other, not a copy.
 */
@Component
public class BoardPushService {

    private final SimpMessagingTemplate messagingTemplate;

    public BoardPushService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void onIssueCreated(IssueCreatedEvent event) {
        push(event.projectId(), "ISSUE_CREATED", Map.of("issueId", event.issueId().toString()));
    }

    @EventListener
    public void onIssueStatusChanged(IssueStatusChangedEvent event) {
        push(event.projectId(), "ISSUE_STATUS_CHANGED", Map.of(
                "issueId", event.issueId().toString(), "fromStatus", event.fromStatus(), "toStatus", event.toStatus()));
    }

    @EventListener
    public void onIssueAssigned(IssueAssignedEvent event) {
        push(event.projectId(), "ISSUE_ASSIGNED", Map.of("issueId", event.issueId().toString(), "assigneeId", event.assigneeId().toString()));
    }

    private void push(java.util.UUID projectId, String type, Map<String, Object> payload) {
        Map<String, Object> message = Map.of("type", type, "data", payload);
        for (String boardType : java.util.List.of("SCRUM", "KANBAN")) {
            messagingTemplate.convertAndSend("/topic/projects/" + projectId + "/boards/" + boardType, message);
        }
    }
}

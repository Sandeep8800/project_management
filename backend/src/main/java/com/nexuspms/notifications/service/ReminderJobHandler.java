package com.nexuspms.notifications.service;

import com.nexuspms.common.job.JobHandler;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * LLD S10: handles REMINDER jobs enqueued by SprintReminderScheduler (Sprint &
 * Board module, sprint-ending-soon reminders per PRD FR-39). Notifications owns
 * the handler since dispatch is its concern; Sprint & Board only decides *when*
 * a reminder is due, not how it's delivered.
 */
@Component
public class ReminderJobHandler implements JobHandler {

    public static final String JOB_TYPE = "REMINDER";

    private final NotificationDispatchService notificationDispatchService;

    public ReminderJobHandler(NotificationDispatchService notificationDispatchService) {
        this.notificationDispatchService = notificationDispatchService;
    }

    @Override
    public String jobType() {
        return JOB_TYPE;
    }

    @Override
    public void handle(Map<String, Object> payload) {
        UUID projectId = UUID.fromString((String) payload.get("projectId"));
        String reminderType = (String) payload.get("reminderType");
        notificationDispatchService.notifyAllProjectMembers(projectId, reminderType, payload);
    }
}

package com.nexuspms.sprintboard.service;

import com.nexuspms.common.job.BackgroundJobService;
import com.nexuspms.sprintboard.domain.Sprint;
import com.nexuspms.sprintboard.domain.SprintStatus;
import com.nexuspms.sprintboard.repository.SprintRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * PRD FR-39 sprint-end reminders: the piece that was previously missing --
 * ReminderJobHandler (notifications module) had a registered handler but
 * nothing ever enqueued a REMINDER job. This runs daily, finds active sprints
 * ending soon, and enqueues exactly one reminder per sprint (idempotency key
 * is sprintId + reminder type, not sprintId + date, so re-running on
 * subsequent days doesn't create duplicates once the first one has fired).
 */
@Component
public class SprintReminderScheduler {

    public static final String REMINDER_TYPE_ENDING_SOON = "SPRINT_ENDING_SOON";
    private static final int REMIND_DAYS_BEFORE_END = 2;

    private final SprintRepository sprintRepository;
    private final BackgroundJobService backgroundJobService;

    public SprintReminderScheduler(SprintRepository sprintRepository, BackgroundJobService backgroundJobService) {
        this.sprintRepository = sprintRepository;
        this.backgroundJobService = backgroundJobService;
    }

    @Scheduled(cron = "0 0 8 * * *") // once daily at 08:00 server time
    public void enqueueEndingSoonReminders() {
        LocalDate today = LocalDate.now();
        for (Sprint sprint : sprintRepository.findByStatus(SprintStatus.ACTIVE)) {
            if (sprint.getEndDate() == null) {
                continue;
            }
            long daysUntilEnd = ChronoUnit.DAYS.between(today, sprint.getEndDate());
            if (daysUntilEnd >= 0 && daysUntilEnd <= REMIND_DAYS_BEFORE_END) {
                backgroundJobService.enqueue(
                        ReminderJobTypeHolder.JOB_TYPE,
                        sprint.getId() + ":" + REMINDER_TYPE_ENDING_SOON,
                        Map.of("sprintId", sprint.getId().toString(), "projectId", sprint.getProjectId().toString(),
                                "reminderType", REMINDER_TYPE_ENDING_SOON));
            }
        }
    }

    /** Avoids a compile-time dependency on the Notifications module just for one string constant. */
    private static final class ReminderJobTypeHolder {
        static final String JOB_TYPE = "REMINDER";
    }
}

package com.nexuspms.notifications.service;

import com.nexuspms.common.job.JobHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * LLD S10 job catalog completeness: REMINDER is defined (PRD FR-39 sprint
 * start/end reminders) but nothing currently enqueues one -- immediate
 * sprint-start/sprint-complete notifications are handled directly by
 * NotificationDispatchService's event listeners. A true "reminder N days
 * before sprint end" feature needs a scheduler that computes and enqueues
 * these ahead of time, which isn't built in this pass. This handler is a
 * placeholder so the job type has a registered handler (BackgroundJobWorker
 * logs a warning and no-ops for any job type without one) rather than silently
 * failing once that scheduler is added.
 */
@Component
public class ReminderJobHandler implements JobHandler {

    private static final Logger log = LoggerFactory.getLogger(ReminderJobHandler.class);
    public static final String JOB_TYPE = "REMINDER";

    @Override
    public String jobType() {
        return JOB_TYPE;
    }

    @Override
    public void handle(Map<String, Object> payload) {
        log.info("[STUB REMINDER] payload {} -- no scheduler enqueues this job type yet", payload);
    }
}

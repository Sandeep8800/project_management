package com.nexuspms.common.job;

import java.util.Map;

/**
 * LLD S10 job catalog: each module that owns a job type (SPRINT_ROLLOVER ->
 * Sprint & Board, NOTIFICATION_DISPATCH -> Notifications, PROJECTION_REBUILD ->
 * Reporting, REMINDER -> Notifications) contributes one bean implementing this,
 * discovered generically by BackgroundJobWorker. No module needs to know about
 * any other module's job types.
 */
public interface JobHandler {

    String jobType();

    void handle(Map<String, Object> payload);
}

package com.nexuspms.common.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * HLD S8.5: polls the durable job table and dispatches to whichever module's
 * JobHandler owns that job type. Runs in-process, inside the same Spring Boot
 * application (consistent with the modular-monolith decision, HLD S2) -- not a
 * separate worker service.
 */
@Component
public class BackgroundJobWorker {

    private static final Logger log = LoggerFactory.getLogger(BackgroundJobWorker.class);
    private static final int MAX_ATTEMPTS = 5;
    private static final int BATCH_SIZE = 20;

    private final BackgroundJobRepository backgroundJobRepository;
    private final Map<String, JobHandler> handlersByJobType;

    public BackgroundJobWorker(BackgroundJobRepository backgroundJobRepository, List<JobHandler> handlers) {
        this.backgroundJobRepository = backgroundJobRepository;
        this.handlersByJobType = handlers.stream().collect(Collectors.toMap(JobHandler::jobType, h -> h));
    }

    @Scheduled(fixedDelayString = "PT5S")
    public void pollAndDispatch() {
        List<BackgroundJob> due = backgroundJobRepository.findDue(
                List.of(JobStatus.PENDING, JobStatus.FAILED), Instant.now(), PageRequest.of(0, BATCH_SIZE));
        for (BackgroundJob job : due) {
            processOne(job.getId());
        }
    }

    @Transactional
    void processOne(java.util.UUID jobId) {
        BackgroundJob job = backgroundJobRepository.findById(jobId).orElse(null);
        if (job == null || job.getStatus() == JobStatus.COMPLETED) {
            return;
        }
        JobHandler handler = handlersByJobType.get(job.getJobType());
        if (handler == null) {
            log.warn("No JobHandler registered for job type {}", job.getJobType());
            return;
        }
        job.markInProgress();
        try {
            handler.handle(job.getPayload());
            job.markCompleted();
        } catch (Exception ex) {
            log.error("Job {} ({}) failed on attempt {}", job.getId(), job.getJobType(), job.getAttemptCount() + 1, ex);
            if (job.getAttemptCount() + 1 >= MAX_ATTEMPTS) {
                // Parked: still visible as FAILED, but pushed far enough out that
                // the poller stops picking it up -- avoids retrying forever.
                job.markFailed(Instant.now().plus(3650, ChronoUnit.DAYS));
            } else {
                job.markFailed(Instant.now().plus(1L << job.getAttemptCount(), ChronoUnit.MINUTES));
            }
        }
    }
}

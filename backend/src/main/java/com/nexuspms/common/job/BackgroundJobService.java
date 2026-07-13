package com.nexuspms.common.job;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class BackgroundJobService {

    private final BackgroundJobRepository backgroundJobRepository;

    public BackgroundJobService(BackgroundJobRepository backgroundJobRepository) {
        this.backgroundJobRepository = backgroundJobRepository;
    }

    /** Idempotent enqueue: a duplicate (jobType, idempotencyKey) returns the existing job rather than erroring (LLD S10). */
    @Transactional
    public BackgroundJob enqueue(String jobType, String idempotencyKey, Map<String, Object> payload) {
        return backgroundJobRepository.findByJobTypeAndIdempotencyKey(jobType, idempotencyKey)
                .orElseGet(() -> backgroundJobRepository.save(new BackgroundJob(jobType, idempotencyKey, payload)));
    }
}

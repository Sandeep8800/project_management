package com.nexuspms.common.job;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * HLD S8.5 / LLD S10 / Database Design S9.3: durable job table, polled by
 * BackgroundJobWorker. Idempotency is a DB-enforced fact via the unique
 * (job_type, idempotency_key) constraint, not an application convention.
 *
 * This lives in `common`, not any one business module, because background jobs
 * are a cross-cutting mechanism multiple modules enqueue into and handle (Sprint
 * & Board's rollover, Notifications' dispatch, Reporting's projection rebuild) --
 * owning it in one specific module would create exactly the kind of dependency
 * inversion the module boundaries elsewhere in this codebase were built to avoid.
 */
@Entity
@Table(name = "background_jobs")
public class BackgroundJob {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "job_type", nullable = false)
    private String jobType;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, Object> payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status = JobStatus.PENDING;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_run_at", nullable = false)
    private Instant nextRunAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BackgroundJob() {
        // JPA
    }

    public BackgroundJob(String jobType, String idempotencyKey, Map<String, Object> payload) {
        this.jobType = jobType;
        this.idempotencyKey = idempotencyKey;
        this.payload = payload;
        this.nextRunAt = Instant.now();
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (nextRunAt == null) nextRunAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void markInProgress() {
        this.status = JobStatus.IN_PROGRESS;
    }

    public void markCompleted() {
        this.status = JobStatus.COMPLETED;
    }

    public void markFailed(Instant nextAttemptAt) {
        this.status = JobStatus.FAILED;
        this.attemptCount++;
        this.nextRunAt = nextAttemptAt;
    }

    public void retry() {
        this.status = JobStatus.PENDING;
    }

    public UUID getId() {
        return id;
    }

    public String getJobType() {
        return jobType;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public JobStatus getStatus() {
        return status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public Instant getNextRunAt() {
        return nextRunAt;
    }
}

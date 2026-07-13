-- Database Design S9: Notifications & Background Jobs
CREATE TABLE notification_outbox (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_id        uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    event_type          varchar(64) NOT NULL,
    payload             jsonb NOT NULL,
    channel             varchar(16) NOT NULL CHECK (channel IN ('IN_APP', 'EMAIL')),
    status               varchar(16) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'DEAD_LETTERED')),
    attempt_count        integer NOT NULL DEFAULT 0,
    next_attempt_at      timestamptz NOT NULL DEFAULT now(),
    created_at           timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_notification_outbox_status_next_attempt ON notification_outbox (status, next_attempt_at);

CREATE TABLE notifications (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_id    uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    event_type      varchar(64) NOT NULL,
    payload         jsonb NOT NULL,
    read_at         timestamptz,
    created_at      timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_notifications_recipient_read ON notifications (recipient_id, read_at);

CREATE TABLE background_jobs (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    job_type            varchar(48) NOT NULL CHECK (job_type IN ('SPRINT_ROLLOVER', 'NOTIFICATION_DISPATCH', 'PROJECTION_REBUILD', 'REMINDER')),
    idempotency_key     varchar(255) NOT NULL,
    payload             jsonb NOT NULL,
    status              varchar(16) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED')),
    attempt_count       integer NOT NULL DEFAULT 0,
    next_run_at         timestamptz NOT NULL DEFAULT now(),
    created_at          timestamptz NOT NULL DEFAULT now(),
    updated_at          timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_background_jobs_type_idempotency_key ON background_jobs (job_type, idempotency_key);
CREATE INDEX idx_background_jobs_status_next_run ON background_jobs (status, next_run_at);

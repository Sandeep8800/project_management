-- Database Design S4: Identity & Access
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE users (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name            varchar(255) NOT NULL,
    email           varchar(320) NOT NULL,
    employee_id     varchar(64),
    department      varchar(128),
    default_role    varchar(32) NOT NULL,
    status          varchar(16) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'DEACTIVATED')),
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_users_email_lower ON users (lower(email));
CREATE UNIQUE INDEX uq_users_employee_id ON users (employee_id) WHERE employee_id IS NOT NULL;

CREATE TABLE local_credentials (
    user_id                 uuid PRIMARY KEY REFERENCES users (id) ON DELETE RESTRICT,
    password_hash           varchar(255) NOT NULL,
    password_updated_at     timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE sso_identity_links (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         uuid NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    idp_issuer      varchar(255) NOT NULL,
    idp_subject     varchar(255) NOT NULL,
    linked_at       timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_sso_identity_links_issuer_subject ON sso_identity_links (idp_issuer, idp_subject);
CREATE INDEX idx_sso_identity_links_user ON sso_identity_links (user_id);

CREATE TABLE refresh_tokens (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash      varchar(255) NOT NULL UNIQUE,
    expires_at      timestamptz NOT NULL,
    revoked_at      timestamptz
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);

-- Database Design S5: Governance & RBAC
CREATE TABLE projects (
    id                      uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    project_key             varchar(10) NOT NULL UNIQUE,
    name                    varchar(255) NOT NULL,
    description             text,
    methodology             varchar(16) NOT NULL CHECK (methodology IN ('SCRUM', 'KANBAN', 'HYBRID')),
    start_date              date NOT NULL,
    target_release_date     date,
    status                  varchar(16) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'ARCHIVED', 'DELETED')),
    created_at              timestamptz NOT NULL DEFAULT now(),
    updated_at              timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_projects_status ON projects (status);

CREATE TABLE project_memberships (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         uuid NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    project_id      uuid NOT NULL REFERENCES projects (id) ON DELETE RESTRICT,
    role            varchar(48) NOT NULL,
    assigned_at     timestamptz NOT NULL DEFAULT now(),
    assigned_by     uuid NOT NULL REFERENCES users (id)
);

CREATE UNIQUE INDEX uq_project_memberships_user_project ON project_memberships (user_id, project_id);
CREATE INDEX idx_project_memberships_project ON project_memberships (project_id);
CREATE INDEX idx_project_memberships_user ON project_memberships (user_id);

-- LLD S4.1 / Database Design S5.3: fixed role/permission matrix, seed data only, never
-- editable through the application in v1.
CREATE TABLE role_permissions (
    role        varchar(48) NOT NULL,
    permission  varchar(48) NOT NULL,
    PRIMARY KEY (role, permission),
    CONSTRAINT chk_role_permissions_role CHECK (role IN (
        'ADMIN', 'PROJECT_MANAGER_SCRUM_MASTER', 'PRODUCT_OWNER',
        'DEVELOPER', 'QA_TESTER', 'VIEWER_STAKEHOLDER'
    )),
    CONSTRAINT chk_role_permissions_permission CHECK (permission IN (
        'CREATE_ISSUE', 'EDIT_ISSUE', 'TRANSITION_STATUS', 'DELETE_ISSUE',
        'MANAGE_SPRINT', 'CONFIGURE_BOARD', 'VIEW_REPORTS', 'MANAGE_PROJECT_USERS'
    ))
);

CREATE TABLE audit_log (
    id                      uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id                uuid NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    action_type             varchar(64) NOT NULL,
    target_entity_type      varchar(64) NOT NULL,
    target_entity_id        uuid NOT NULL,
    metadata                jsonb,
    created_at              timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_log_actor ON audit_log (actor_id);
CREATE INDEX idx_audit_log_action_type ON audit_log (action_type);
CREATE INDEX idx_audit_log_created_at ON audit_log (created_at);
CREATE INDEX idx_audit_log_target ON audit_log (target_entity_type, target_entity_id);

-- Append-only: revoke UPDATE/DELETE from the application role (Database Design S5.4).
-- The application connects as nexuspms_app; adjust the role name to match the actual
-- deployment's runtime DB user before running in a real environment.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'nexuspms_app') THEN
        REVOKE UPDATE, DELETE ON audit_log FROM nexuspms_app;
    END IF;
END $$;

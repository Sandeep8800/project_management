-- Database Design S7: Sprint & Board (incl. Workflow Engine)
CREATE TABLE sprints (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id      uuid NOT NULL REFERENCES projects (id) ON DELETE RESTRICT,
    name            varchar(255) NOT NULL,
    goal            text,
    start_date      date,
    end_date        date,
    status          varchar(16) NOT NULL DEFAULT 'PLANNED' CHECK (status IN ('PLANNED', 'ACTIVE', 'COMPLETED')),
    version         integer NOT NULL DEFAULT 0,
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_sprints_project_status ON sprints (project_id, status);

-- LLD S6.1 / S12: DB-enforced single-active-sprint-per-project invariant, beneath
-- the application-level check -- defense in depth against a race the app misses.
CREATE UNIQUE INDEX uq_sprints_one_active_per_project ON sprints (project_id) WHERE status = 'ACTIVE';

ALTER TABLE issues ADD CONSTRAINT fk_issues_sprint FOREIGN KEY (sprint_id) REFERENCES sprints (id) ON DELETE SET NULL;

CREATE TABLE workflow_definitions (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id      uuid NOT NULL UNIQUE REFERENCES projects (id) ON DELETE CASCADE
);

CREATE TABLE workflow_statuses (
    id                          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_definition_id      uuid NOT NULL REFERENCES workflow_definitions (id) ON DELETE CASCADE,
    name                        varchar(64) NOT NULL,
    display_order               integer NOT NULL,
    is_initial                  boolean NOT NULL DEFAULT false,
    is_terminal                 boolean NOT NULL DEFAULT false
);

CREATE UNIQUE INDEX uq_workflow_statuses_definition_name ON workflow_statuses (workflow_definition_id, name);

CREATE TABLE workflow_transitions (
    id                          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_definition_id      uuid NOT NULL REFERENCES workflow_definitions (id) ON DELETE CASCADE,
    from_status_id              uuid NOT NULL REFERENCES workflow_statuses (id) ON DELETE CASCADE,
    to_status_id                uuid NOT NULL REFERENCES workflow_statuses (id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX uq_workflow_transitions_from_to ON workflow_transitions (from_status_id, to_status_id);

CREATE TABLE boards (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id      uuid NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    board_type      varchar(16) NOT NULL CHECK (board_type IN ('SCRUM', 'KANBAN'))
);

CREATE UNIQUE INDEX uq_boards_project_type ON boards (project_id, board_type);

CREATE TABLE board_columns (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    board_id        uuid NOT NULL REFERENCES boards (id) ON DELETE CASCADE,
    name            varchar(64) NOT NULL,
    display_order   integer NOT NULL,
    wip_limit       integer
);

CREATE TABLE board_column_statuses (
    board_column_id     uuid NOT NULL REFERENCES board_columns (id) ON DELETE CASCADE,
    workflow_status_id  uuid NOT NULL REFERENCES workflow_statuses (id) ON DELETE RESTRICT,
    PRIMARY KEY (board_column_id, workflow_status_id)
);

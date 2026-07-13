-- Database Design S8: Reporting projections. Write targets only for
-- ProjectionUpdateService (LLD S7) -- never written directly by request handlers.
CREATE TABLE burndown_snapshots (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    sprint_id           uuid NOT NULL REFERENCES sprints (id) ON DELETE CASCADE,
    snapshot_date       date NOT NULL,
    remaining_points    numeric(6, 1) NOT NULL,
    remaining_issue_count integer NOT NULL
);

CREATE UNIQUE INDEX uq_burndown_snapshots_sprint_date ON burndown_snapshots (sprint_id, snapshot_date);

CREATE TABLE velocity_data_points (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id          uuid NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    sprint_id           uuid NOT NULL UNIQUE REFERENCES sprints (id) ON DELETE CASCADE,
    committed_points    numeric(6, 1) NOT NULL,
    completed_points    numeric(6, 1) NOT NULL
);

CREATE INDEX idx_velocity_data_points_project ON velocity_data_points (project_id);

CREATE TABLE cfd_snapshots (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    board_id        uuid NOT NULL REFERENCES boards (id) ON DELETE CASCADE,
    snapshot_date   date NOT NULL,
    status_name     varchar(64) NOT NULL,
    issue_count     integer NOT NULL
);

CREATE UNIQUE INDEX uq_cfd_snapshots_board_date_status ON cfd_snapshots (board_id, snapshot_date, status_name);

CREATE TABLE sprint_summaries (
    sprint_id               uuid PRIMARY KEY REFERENCES sprints (id) ON DELETE CASCADE,
    planned_points          numeric(6, 1) NOT NULL,
    completed_points        numeric(6, 1) NOT NULL,
    scope_added_points      numeric(6, 1) NOT NULL DEFAULT 0,
    scope_removed_points    numeric(6, 1) NOT NULL DEFAULT 0,
    carry_over_issue_count  integer NOT NULL DEFAULT 0
);

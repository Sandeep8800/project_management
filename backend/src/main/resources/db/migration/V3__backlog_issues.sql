-- Database Design S6: Backlog & Issues
-- Sprint/board tables are created in V4; issues.sprint_id FK is added there to avoid
-- a forward reference, but the column itself lives here since it's core to Issue.

CREATE TABLE issues (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id          uuid NOT NULL REFERENCES projects (id) ON DELETE RESTRICT,
    issue_key           varchar(20) NOT NULL UNIQUE,
    issue_type          varchar(16) NOT NULL CHECK (issue_type IN ('EPIC', 'STORY', 'TASK', 'SUBTASK', 'BUG')),
    parent_issue_id     uuid REFERENCES issues (id) ON DELETE RESTRICT,
    title               varchar(500) NOT NULL,
    description         text,
    status              varchar(64) NOT NULL,
    assignee_id         uuid REFERENCES users (id) ON DELETE SET NULL,
    reporter_id         uuid NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    priority            varchar(16) NOT NULL,
    story_points         numeric(4, 1),
    sprint_id           uuid, -- FK added in V4 once sprints exists
    backlog_rank        numeric NOT NULL,
    search_vector       tsvector GENERATED ALWAYS AS (
                             setweight(to_tsvector('english', coalesce(title, '')), 'A') ||
                             setweight(to_tsvector('english', coalesce(description, '')), 'B')
                         ) STORED,
    version             integer NOT NULL DEFAULT 0,
    created_at          timestamptz NOT NULL DEFAULT now(),
    updated_at          timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_issues_project_status ON issues (project_id, status);
CREATE INDEX idx_issues_project_sprint ON issues (project_id, sprint_id);
CREATE INDEX idx_issues_assignee ON issues (assignee_id);
CREATE INDEX idx_issues_parent ON issues (parent_issue_id);
CREATE INDEX idx_issues_project_rank ON issues (project_id, backlog_rank);
CREATE INDEX idx_issues_search_vector ON issues USING GIN (search_vector);

CREATE TABLE issue_links (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    source_issue_id     uuid NOT NULL REFERENCES issues (id) ON DELETE CASCADE,
    target_issue_id     uuid NOT NULL REFERENCES issues (id) ON DELETE CASCADE,
    link_type           varchar(24) NOT NULL CHECK (link_type IN ('BLOCKS', 'IS_BLOCKED_BY', 'RELATES_TO', 'DUPLICATES'))
);

CREATE INDEX idx_issue_links_source ON issue_links (source_issue_id);
CREATE INDEX idx_issue_links_target ON issue_links (target_issue_id);

CREATE TABLE comments (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    issue_id        uuid NOT NULL REFERENCES issues (id) ON DELETE CASCADE,
    author_id       uuid NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    body            text NOT NULL,
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_comments_issue_created ON comments (issue_id, created_at);

CREATE TABLE attachments (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    issue_id        uuid NOT NULL REFERENCES issues (id) ON DELETE CASCADE,
    uploaded_by     uuid NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    file_name       varchar(255) NOT NULL,
    content_type    varchar(128) NOT NULL,
    size_bytes      bigint NOT NULL CHECK (size_bytes <= 26214400),
    storage_key     varchar(512) NOT NULL,
    created_at      timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_attachments_issue ON attachments (issue_id);

CREATE TABLE labels (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id      uuid NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    name            varchar(64) NOT NULL
);

CREATE UNIQUE INDEX uq_labels_project_name ON labels (project_id, name);

CREATE TABLE issue_labels (
    issue_id    uuid NOT NULL REFERENCES issues (id) ON DELETE CASCADE,
    label_id    uuid NOT NULL REFERENCES labels (id) ON DELETE CASCADE,
    PRIMARY KEY (issue_id, label_id)
);

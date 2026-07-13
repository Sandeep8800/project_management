-- Database Design S10: concrete role_permissions seed values.
-- Read access to backlog/board is NOT gated by this table -- it requires only an
-- active project_memberships row of any role. This table governs mutating actions
-- and VIEW_REPORTS specifically.

INSERT INTO role_permissions (role, permission) VALUES
    -- ADMIN: all permissions
    ('ADMIN', 'CREATE_ISSUE'),
    ('ADMIN', 'EDIT_ISSUE'),
    ('ADMIN', 'TRANSITION_STATUS'),
    ('ADMIN', 'DELETE_ISSUE'),
    ('ADMIN', 'MANAGE_SPRINT'),
    ('ADMIN', 'CONFIGURE_BOARD'),
    ('ADMIN', 'VIEW_REPORTS'),
    ('ADMIN', 'MANAGE_PROJECT_USERS'),

    -- PROJECT_MANAGER_SCRUM_MASTER: everything except MANAGE_PROJECT_USERS
    -- (Database Design S10 design note: project membership stays Admin-only,
    -- consistent with the PRD's admin-governed philosophy).
    ('PROJECT_MANAGER_SCRUM_MASTER', 'CREATE_ISSUE'),
    ('PROJECT_MANAGER_SCRUM_MASTER', 'EDIT_ISSUE'),
    ('PROJECT_MANAGER_SCRUM_MASTER', 'TRANSITION_STATUS'),
    ('PROJECT_MANAGER_SCRUM_MASTER', 'DELETE_ISSUE'),
    ('PROJECT_MANAGER_SCRUM_MASTER', 'MANAGE_SPRINT'),
    ('PROJECT_MANAGER_SCRUM_MASTER', 'CONFIGURE_BOARD'),
    ('PROJECT_MANAGER_SCRUM_MASTER', 'VIEW_REPORTS'),

    -- PRODUCT_OWNER: backlog ownership, no delete/sprint-management/board-config
    ('PRODUCT_OWNER', 'CREATE_ISSUE'),
    ('PRODUCT_OWNER', 'EDIT_ISSUE'),
    ('PRODUCT_OWNER', 'TRANSITION_STATUS'),
    ('PRODUCT_OWNER', 'VIEW_REPORTS'),

    -- DEVELOPER: works issues, no delete/sprint-management/board-config
    ('DEVELOPER', 'CREATE_ISSUE'),
    ('DEVELOPER', 'EDIT_ISSUE'),
    ('DEVELOPER', 'TRANSITION_STATUS'),
    ('DEVELOPER', 'VIEW_REPORTS'),

    -- QA_TESTER: same coarse-grained permission set as Developer at this
    -- role-matrix granularity; the distinction is workflow ownership of the
    -- bug lifecycle, not a different permission bit set (Database Design S10).
    ('QA_TESTER', 'CREATE_ISSUE'),
    ('QA_TESTER', 'EDIT_ISSUE'),
    ('QA_TESTER', 'TRANSITION_STATUS'),
    ('QA_TESTER', 'VIEW_REPORTS'),

    -- VIEWER_STAKEHOLDER: read-only (PRD FR-38)
    ('VIEWER_STAKEHOLDER', 'VIEW_REPORTS');

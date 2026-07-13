package com.nexuspms.governance.domain;

/** LLD S4.1 / PRD FR-13. Read access to backlog/board is NOT gated by this enum -- it requires only an active ProjectMembership of any role. */
public enum Permission {
    CREATE_ISSUE,
    EDIT_ISSUE,
    TRANSITION_STATUS,
    DELETE_ISSUE,
    MANAGE_SPRINT,
    CONFIGURE_BOARD,
    VIEW_REPORTS,
    MANAGE_PROJECT_USERS
}

package com.nexuspms.governance.domain;

/** LLD S4.1: fixed role set (PRD S3.1.3) -- not admin-editable via the UI in v1. */
public enum Role {
    ADMIN,
    PROJECT_MANAGER_SCRUM_MASTER,
    PRODUCT_OWNER,
    DEVELOPER,
    QA_TESTER,
    VIEWER_STAKEHOLDER
}

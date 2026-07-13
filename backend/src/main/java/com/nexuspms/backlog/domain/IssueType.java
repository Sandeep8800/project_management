package com.nexuspms.backlog.domain;

/** PRD FR-18: Epic -> Story -> Task -> Sub-task hierarchy, plus Bug as a first-class type. */
public enum IssueType {
    EPIC,
    STORY,
    TASK,
    SUBTASK,
    BUG
}

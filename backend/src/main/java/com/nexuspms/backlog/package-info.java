/**
 * Backlog & Issues module (HLD S4, LLD S5, Database Design S6, API Design S6).
 *
 * Owns Epics/Stories/Tasks/Sub-tasks/Bugs, issue links, comments, attachments,
 * labels, and the shared per-project backlog ordering. Depends on Governance &
 * RBAC (authorization) and Identity & Access (assignee/reporter) per the HLD
 * S4 module dependency table.
 *
 * Known follow-up work: attachment storage is a local-disk stand-in, not real
 * S3 (see AttachmentService); issue-key generation is not race-safe under
 * concurrent creates on the same project across multiple app instances (see
 * IssueKeyGenerator).
 */
package com.nexuspms.backlog;

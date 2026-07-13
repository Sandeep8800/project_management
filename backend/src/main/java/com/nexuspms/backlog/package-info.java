/**
 * Backlog & Issues module (HLD S4, LLD S5, Database Design S6, API Design S6).
 *
 * SCAFFOLDED, NOT IMPLEMENTED in this pass. Owns Epics/Stories/Tasks/Sub-tasks/
 * Bugs, issue links, comments, attachments, labels, and the shared per-project
 * backlog ordering. Depends on Governance & RBAC (authorization) and Identity &
 * Access (assignee/reporter) per the HLD S4 module dependency table.
 *
 * Follow-up work: Issue/IssueLink/Comment/Attachment/Label entities (schema
 * already exists, Database Design S6), IssueService/BacklogService/CommentService/
 * AttachmentService (LLD S5), optimistic-lock conflict handling on PATCH, the
 * pre-signed-upload-URL attachment flow, and IssueSearchService as the seam for
 * a future search-engine swap (HLD S5).
 */
package com.nexuspms.backlog;

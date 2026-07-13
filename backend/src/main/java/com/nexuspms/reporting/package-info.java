/**
 * Reporting module (HLD S4/S5/S8.2, LLD S7, Database Design S8, API Design S8).
 *
 * SCAFFOLDED, NOT IMPLEMENTED in this pass. Owns burndown/velocity/CFD/sprint-
 * summary projections -- always pre-aggregated, never computed live from raw
 * issue history at read time.
 *
 * Follow-up work: ProjectionUpdateService reacting to IssueStatusChanged/
 * SprintStarted/SprintCompleted (requires Backlog & Issues and Sprint & Board to
 * exist first and actually publish those events), SprintSummaryService, and the
 * read-only ReportQueryService (no write access at all, structurally enforcing
 * the Viewer/Stakeholder read-only guarantee, PRD FR-38).
 */
package com.nexuspms.reporting;

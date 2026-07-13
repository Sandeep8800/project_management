/**
 * Sprint & Board module, including the Workflow Engine sub-component (HLD S4,
 * LLD S6, Database Design S7, API Design S7).
 *
 * SCAFFOLDED, NOT IMPLEMENTED in this pass. Owns sprint lifecycle (the
 * PLANNED/ACTIVE/COMPLETED state machine, LLD S6.1), Scrum/Kanban board
 * configuration over the shared backlog, WIP limits, and per-project workflow
 * definitions/transitions.
 *
 * Follow-up work: Sprint/WorkflowDefinition/WorkflowStatus/WorkflowTransition/
 * Board/BoardColumn entities (schema already exists, Database Design S7),
 * SprintService (start/complete with the required non-defaulted rollover
 * decision, PRD FR-25), WorkflowTransitionValidator (LLD S6.2, checked after
 * RBAC, never before), and BoardService with WIP-limit enforcement.
 */
package com.nexuspms.sprintboard;

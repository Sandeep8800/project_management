# Nexus PMS — Database Design

**Status:** Draft for review
**Phase:** 4 of 7 — Documentation-first sequence (PRD → HLD → LLD → **Database Design** → API Design → UI Design → Coding)
**Input:** [`01-PRD.md`](./01-PRD.md) · [`02-HLD.md`](./02-HLD.md) · [`03-LLD.md`](./03-LLD.md) (all Approved)
**Owners:** Architecture / Database
**Last updated:** 2026-07-13

> This document defines concrete PostgreSQL schema — tables, columns, types, constraints, indexes — for every entity named in the LLD. It does not define REST contracts (API Design phase) or UI (UI Design phase). No implementation code (migrations, ORM entities) is written against this phase; this is the schema specification those artifacts will implement.

---

## 1. Purpose & Scope

Turn every conceptual entity from LLD §3–§10 into a concrete table definition, sized and indexed against:
- The Large-scale target (HLD §1): 300+ concurrent users, 100+ projects, 10k+ issues/project (~1M+ issues at ceiling).
- The HLD §13 NFR targets (P95 API latency < 300 ms reads, board load < 1.5 s, report generation < 1 s from projections).
- The LLD §12 concurrency rules (optimistic locking, single-active-sprint invariant) — enforced here as DB-level constraints wherever possible, as a safety net beneath the application-level checks, not a replacement for them.

---

## 2. Conventions

| Convention | Choice | Rationale |
|---|---|---|
| Primary keys | `UUID` (v7 / time-ordered where the driver supports it) | Avoids sequential-ID enumeration across a single-tenant-but-multi-project system; time-ordered UUIDs keep index locality reasonable (unlike random v4) for the Large-scale insert rate. |
| Naming | `snake_case`, **plural** table names (`issues`, `projects`), singular column names | Consistent with common PostgreSQL/JPA convention; avoids ambiguity between entity name and table name in ORM mapping. |
| Audit columns | Every table: `created_at timestamptz not null default now()`, `updated_at timestamptz not null default now()` (trigger-maintained) | Baseline traceability independent of the dedicated `audit_log` table, which covers governance actions specifically (LLD §4.4). |
| Optimistic locking | `version integer not null default 0` on tables identified in LLD §12 (`issues`, `sprints`) | Backs the `ConcurrentModificationException` behavior from LLD §5/§12. |
| Soft state vs. hard delete | `status` enum column (e.g., `ACTIVE`/`DEACTIVATED`, `PLANNED`/`ARCHIVED`/`DELETED`) on `users` and `projects` — no hard `DELETE` in the application write paths for these two tables | Matches PRD's traceability requirement (FR-5: deactivated users stay visible in history) and the archive-before-delete safeguard (FR-8). |
| Foreign keys | Always declared, `ON DELETE RESTRICT` by default; `CASCADE` only where explicitly noted below | Prevents accidental orphaning or silent bulk deletion — consistent with "safeguards against destructive loss of history" (PRD §1.3.2). |
| Migrations | Versioned, forward-only migration tool (e.g., Flyway), one migration file per schema change, applied in CI before app deploy | Standard practice; not further specified here — implementation-phase detail. |

---

## 3. Schema Overview

```
users ──┬──< project_memberships >──┬── projects
        │                            │
        ├──< sso_identity_links      ├──< sprints
        ├──< local_credentials       ├──< workflow_definitions ──< workflow_statuses
        ├──< refresh_tokens          │                          └< workflow_transitions
        │                            ├──< boards ──< board_columns >── workflow_statuses
        └──< audit_log (actor_id)    │
                                     └──< issues >── sprints (nullable FK)
                                              │
                                              ├──< issue_links (self-referencing, typed)
                                              ├──< comments
                                              ├──< attachments
                                              ├──< issue_labels >── labels
                                              └── parent_issue_id (self-referencing, hierarchy)

issues ──< burndown_snapshots / velocity_data_points / cfd_snapshots / sprint_summaries   (Reporting projections)
issues / sprints / project_memberships ──< notification_outbox ──< notifications
(all mutating tables) ──< background_jobs (polymorphic job payload)
role_permissions (seed reference table, no FK — joins by role/permission enum value)
```

---

## 4. Identity & Access

### 4.1 `users`
| Column | Type | Constraints |
|---|---|---|
| id | uuid | PK |
| name | varchar(255) | not null |
| email | varchar(320) | not null, **unique** |
| employee_id | varchar(64) | unique, nullable (not all orgs use one) |
| department | varchar(128) | nullable |
| default_role | varchar(32) | not null — role enum value, applied as the default at next project assignment (PRD FR-1) |
| status | varchar(16) | not null, `ACTIVE` \| `DEACTIVATED`, default `ACTIVE` |
| created_at / updated_at | timestamptz | per §2 |

Indexes: unique on `email` (case-insensitive — `lower(email)` unique index, since SSO auto-link matching in LLD §11.4 is an exact-email lookup that must be case-insensitive to avoid a real-world linkage miss), unique on `employee_id` where not null.

### 4.2 `local_credentials`
| Column | Type | Constraints |
|---|---|---|
| user_id | uuid | PK, FK → `users.id` ON DELETE RESTRICT |
| password_hash | varchar(255) | not null (bcrypt) |
| password_updated_at | timestamptz | not null |

One-to-one with `users`; absent for SSO-only accounts.

### 4.3 `sso_identity_links`
| Column | Type | Constraints |
|---|---|---|
| id | uuid | PK |
| user_id | uuid | FK → `users.id` ON DELETE RESTRICT, not null |
| idp_issuer | varchar(255) | not null (IdP identifier, e.g. Okta tenant URL) |
| idp_subject | varchar(255) | not null (subject/NameID from the assertion) |
| linked_at | timestamptz | not null |

Indexes: unique on `(idp_issuer, idp_subject)` (one IdP identity links to exactly one user); index on `user_id`. This table is what LLD §11.4's `SsoLinkingService` reads/writes — a `users.email` match with no existing row here triggers link-creation; a match with an existing row for a *different* `idp_subject` triggers the reject-on-conflict path.

### 4.4 `refresh_tokens`
| Column | Type | Constraints |
|---|---|---|
| id | uuid | PK |
| user_id | uuid | FK → `users.id` ON DELETE CASCADE, not null |
| token_hash | varchar(255) | not null, unique (never store raw tokens) |
| expires_at | timestamptz | not null |
| revoked_at | timestamptz | nullable |

Indexes: `(user_id)`, `(expires_at)` for cleanup jobs. `ON DELETE CASCADE` here (unlike the general FK default) is deliberate — a token has no meaning independent of its user, and deactivation already handles the "don't lose history" concern at the `users.status` level, not at the token level.

---

## 5. Governance & RBAC

### 5.1 `projects`
| Column | Type | Constraints |
|---|---|---|
| id | uuid | PK |
| project_key | varchar(10) | not null, **unique**, immutable once `issues` exist (enforced at service layer per LLD §4.3, not a DB trigger, to keep the error message application-controlled) |
| name | varchar(255) | not null |
| description | text | nullable |
| methodology | varchar(16) | not null — `SCRUM` \| `KANBAN` \| `HYBRID` (HLD §4: determines which board types are available; backlog is shared regardless) |
| start_date | date | not null |
| target_release_date | date | nullable |
| status | varchar(16) | not null — `ACTIVE` \| `ARCHIVED` \| `DELETED`, default `ACTIVE` |
| created_at / updated_at | timestamptz | per §2 |

Indexes: unique on `project_key`; index on `status` (admin dashboard filters active vs. archived, PRD FR-16).

### 5.2 `project_memberships`
| Column | Type | Constraints |
|---|---|---|
| id | uuid | PK |
| user_id | uuid | FK → `users.id` ON DELETE RESTRICT, not null |
| project_id | uuid | FK → `projects.id` ON DELETE RESTRICT, not null |
| role | varchar(48) | not null — one of the six PRD role enum values (LLD §4.1) |
| assigned_at | timestamptz | not null |
| assigned_by | uuid | FK → `users.id`, not null (who granted it, feeds audit context) |

Indexes: **unique** `(user_id, project_id)` — one role per user per project (PRD FR-11, exactly one row, not one-per-role); index on `project_id` (per-project membership view, PRD FR-14); index on `user_id` (per-user assignment view, PRD FR-14, and `PermissionResolver` lookups, LLD §4.2).

### 5.3 `role_permissions` (seed reference table)
| Column | Type | Constraints |
|---|---|---|
| role | varchar(48) | not null, part of composite PK |
| permission | varchar(48) | not null, part of composite PK |

Composite PK `(role, permission)`. Not admin-editable at runtime in v1 (LLD §4.1) — populated by migration-seeded data (§9 below), read-only through the application. No FK to an enum table since both are fixed, code-level enums; a CHECK constraint restricts values to the known set so a typo in a future seed migration fails loudly rather than silently creating a dead role/permission pair.

### 5.4 `audit_log`
| Column | Type | Constraints |
|---|---|---|
| id | uuid | PK |
| actor_id | uuid | FK → `users.id` ON DELETE RESTRICT, not null |
| action_type | varchar(64) | not null (e.g., `USER_CREATED`, `PROJECT_ARCHIVED`, `ROLE_ASSIGNED`) |
| target_entity_type | varchar(64) | not null (e.g., `USER`, `PROJECT`, `PROJECT_MEMBERSHIP`) |
| target_entity_id | uuid | not null |
| metadata | jsonb | nullable (e.g., old-role/new-role for a reassignment) |
| created_at | timestamptz | not null, default `now()` |

**Append-only**: no `UPDATE`/`DELETE` grants for the application role on this table — enforced at the database-role level, not just by convention, since PRD FR-17 requires the log to be tamper-evident. Indexes: `(actor_id)`, `(action_type)`, `(created_at)`, `(target_entity_type, target_entity_id)` — directly matching the FR-17 filter set (actor, action type, date range, target entity).

---

## 6. Backlog & Issues

### 6.1 `issues`
Single-table design across all five types (Epic/Story/Task/Sub-task/Bug) with a discriminator column, per LLD §5 — chosen over per-type tables because the types share the overwhelming majority of fields (status, assignee, priority, labels, comments) and cross-type querying (a project's full backlog, regardless of type) is the dominant access pattern; per-type tables would require a union for every backlog/board query.

| Column | Type | Constraints |
|---|---|---|
| id | uuid | PK |
| project_id | uuid | FK → `projects.id` ON DELETE RESTRICT, not null |
| issue_key | varchar(20) | not null, unique — `{project_key}-{sequence}` (e.g., `NEX-142`) |
| issue_type | varchar(16) | not null — `EPIC` \| `STORY` \| `TASK` \| `SUBTASK` \| `BUG` |
| parent_issue_id | uuid | FK → `issues.id` ON DELETE RESTRICT, nullable — hierarchy link (LLD §5); required for `SUBTASK`, optional for `STORY`→Epic and `BUG`→Story/Epic, enforced at service layer per issue type rule |
| title | varchar(500) | not null |
| description | text | nullable |
| status | varchar(64) | not null — references a status defined in the project's `workflow_statuses` (not a DB FK, since workflow statuses are per-project-configurable text values; validated at the service layer via `WorkflowTransitionValidator`, LLD §6.2) |
| assignee_id | uuid | FK → `users.id` ON DELETE SET NULL, nullable |
| reporter_id | uuid | FK → `users.id` ON DELETE RESTRICT, not null |
| priority | varchar(16) | not null |
| story_points | numeric(4,1) | nullable |
| sprint_id | uuid | FK → `sprints.id` ON DELETE SET NULL, nullable — null means "in backlog, not yet in a sprint" |
| backlog_rank | numeric | not null — fractional-indexing rank for drag-reorder (PRD FR-19) within the project's single backlog, avoiding a full-table renumber on every reorder |
| search_vector | tsvector | generated column from `title || description` (v1 Postgres FTS, LLD/HLD §5 search seam) |
| version | integer | not null, default 0 — optimistic lock (LLD §5, §12) |
| created_at / updated_at | timestamptz | per §2 |

Indexes:
- `(project_id, status)` — board/backlog filtering by status within a project, the single most frequent query pattern.
- `(project_id, sprint_id)` — Scrum board (active sprint's issues) and backlog (`sprint_id IS NULL`) views.
- `(assignee_id)` — "my issues" queries.
- `(parent_issue_id)` — hierarchy traversal (Epic→Stories, Story→Sub-tasks).
- `(project_id, backlog_rank)` — ordered backlog retrieval.
- GIN index on `search_vector` — full-text search (§5 seam; swapped for an external engine without a schema change to this table if the HLD §5 trigger is hit, since callers already go through `IssueSearchService`).
- Unique on `issue_key`.

### 6.2 `issue_links`
| Column | Type | Constraints |
|---|---|---|
| id | uuid | PK |
| source_issue_id | uuid | FK → `issues.id` ON DELETE CASCADE, not null |
| target_issue_id | uuid | FK → `issues.id` ON DELETE CASCADE, not null |
| link_type | varchar(24) | not null — `BLOCKS` \| `IS_BLOCKED_BY` \| `RELATES_TO` \| `DUPLICATES` |

`ON DELETE CASCADE` here (unlike `issues.parent_issue_id`'s RESTRICT) is deliberate: a link is a relationship *about* two issues, not load-bearing history the way a parent/child hierarchy or an audit record is — if either issue is deleted the link is meaningless, whereas hierarchy deletion should be blocked to force explicit re-parenting first. Index on `(source_issue_id)`, `(target_issue_id)`.

### 6.3 `comments`
| Column | Type | Constraints |
|---|---|---|
| id | uuid | PK |
| issue_id | uuid | FK → `issues.id` ON DELETE CASCADE, not null |
| author_id | uuid | FK → `users.id` ON DELETE RESTRICT, not null |
| body | text | not null |
| created_at / updated_at | timestamptz | per §2 |

Index on `(issue_id, created_at)` for chronological retrieval. Mention extraction (LLD §5) happens at write time in the service layer, not via a DB trigger — mentions are represented as `UserMentioned` domain events, not persisted rows in this table.

### 6.4 `attachments`
| Column | Type | Constraints |
|---|---|---|
| id | uuid | PK |
| issue_id | uuid | FK → `issues.id` ON DELETE CASCADE, not null |
| uploaded_by | uuid | FK → `users.id` ON DELETE RESTRICT, not null |
| file_name | varchar(255) | not null |
| content_type | varchar(128) | not null |
| size_bytes | bigint | not null, CHECK `size_bytes <= 26214400` (25 MB, HLD §13 proposed limit) |
| storage_key | varchar(512) | not null — object storage reference, never the file bytes (HLD §5) |
| created_at | timestamptz | per §2 |

Index on `(issue_id)`.

### 6.5 `labels` / `issue_labels`
| `labels` | Type | Constraints |
|---|---|---|
| id | uuid | PK |
| project_id | uuid | FK → `projects.id` ON DELETE CASCADE, not null |
| name | varchar(64) | not null |

Unique `(project_id, name)`.

| `issue_labels` | Type | Constraints |
|---|---|---|
| issue_id | uuid | FK → `issues.id` ON DELETE CASCADE |
| label_id | uuid | FK → `labels.id` ON DELETE CASCADE |

Composite PK `(issue_id, label_id)`.

---

## 7. Sprint & Board (incl. Workflow Engine)

### 7.1 `sprints`
| Column | Type | Constraints |
|---|---|---|
| id | uuid | PK |
| project_id | uuid | FK → `projects.id` ON DELETE RESTRICT, not null |
| name | varchar(255) | not null |
| goal | text | nullable |
| start_date | date | nullable (set on `start()`, not at creation — PLANNED sprints per LLD §6.1 may not have committed dates yet) |
| end_date | date | nullable |
| status | varchar(16) | not null — `PLANNED` \| `ACTIVE` \| `COMPLETED` |
| version | integer | not null, default 0 — optimistic lock (LLD §12) |
| created_at / updated_at | timestamptz | per §2 |

Indexes: `(project_id, status)`. **Partial unique index** enforcing the single-active-sprint invariant at the database level as a safety net beneath the application-level check (LLD §6.1, §12):
```sql
CREATE UNIQUE INDEX uq_sprints_one_active_per_project
  ON sprints (project_id)
  WHERE status = 'ACTIVE';
```
This makes the "one active sprint per project" rule impossible to violate even under a race the application-level lock somehow misses — a genuine defense-in-depth addition beyond what LLD specified procedurally.

### 7.2 `workflow_definitions`, `workflow_statuses`, `workflow_transitions`
| `workflow_definitions` | Type | Constraints |
|---|---|---|
| id | uuid | PK |
| project_id | uuid | FK → `projects.id` ON DELETE CASCADE, not null, **unique** (one workflow definition per project in v1 — PRD FR-30 doesn't ask for multiple workflows per project, e.g. per issue type) |

| `workflow_statuses` | Type | Constraints |
|---|---|---|
| id | uuid | PK |
| workflow_definition_id | uuid | FK → `workflow_definitions.id` ON DELETE CASCADE, not null |
| name | varchar(64) | not null |
| display_order | integer | not null |
| is_initial | boolean | not null, default false (exactly one per workflow, enforced at service layer) |
| is_terminal | boolean | not null, default false (e.g., "Done") |

Unique `(workflow_definition_id, name)`.

| `workflow_transitions` | Type | Constraints |
|---|---|---|
| id | uuid | PK |
| workflow_definition_id | uuid | FK → `workflow_definitions.id` ON DELETE CASCADE, not null |
| from_status_id | uuid | FK → `workflow_statuses.id` ON DELETE CASCADE, not null |
| to_status_id | uuid | FK → `workflow_statuses.id` ON DELETE CASCADE, not null |

Unique `(from_status_id, to_status_id)`. This is the table `WorkflowTransitionValidator` (LLD §6.2) queries — note `issues.status` (§6.1) stores the status **name**, not a FK to `workflow_statuses.id`; validated against this table at write time rather than FK-constrained, because changing a project's workflow definition must not retroactively invalidate existing issues sitting in a status that was since renamed or removed (a real operational scenario for a long-lived project).

### 7.3 `boards`, `board_columns`
| `boards` | Type | Constraints |
|---|---|---|
| id | uuid | PK |
| project_id | uuid | FK → `projects.id` ON DELETE CASCADE, not null |
| board_type | varchar(16) | not null — `SCRUM` \| `KANBAN` |

Unique `(project_id, board_type)` — at most one board of each type per project, consistent with HLD §4's "boards are views over one backlog," not separate configurable board instances in v1.

| `board_columns` | Type | Constraints |
|---|---|---|
| id | uuid | PK |
| board_id | uuid | FK → `boards.id` ON DELETE CASCADE, not null |
| name | varchar(64) | not null |
| display_order | integer | not null |
| wip_limit | integer | nullable — Kanban only (PRD FR-26); null/ignored for Scrum boards |

| `board_column_statuses` | Type | Constraints |
|---|---|---|
| board_column_id | uuid | FK → `board_columns.id` ON DELETE CASCADE |
| workflow_status_id | uuid | FK → `workflow_statuses.id` ON DELETE RESTRICT |

Composite PK `(board_column_id, workflow_status_id)` — many-to-many, since a board column can collapse multiple workflow statuses into one visual column (LLD §6.2's example: "In Review" + "QA Review" → one Kanban column), and the same status can appear on both a Scrum and Kanban board's columns independently.

---

## 8. Reporting (Projection Tables)

All four tables here are **write targets only for `ProjectionUpdateService`** (LLD §7) — never written by request-handling controllers directly, and read exclusively from replicas (HLD §5).

### 8.1 `burndown_snapshots`
| Column | Type | Constraints |
|---|---|---|
| id | uuid | PK |
| sprint_id | uuid | FK → `sprints.id` ON DELETE CASCADE, not null |
| snapshot_date | date | not null |
| remaining_points | numeric(6,1) | not null |
| remaining_issue_count | integer | not null |

Unique `(sprint_id, snapshot_date)`.

### 8.2 `velocity_data_points`
| Column | Type | Constraints |
|---|---|---|
| id | uuid | PK |
| project_id | uuid | FK → `projects.id` ON DELETE CASCADE, not null |
| sprint_id | uuid | FK → `sprints.id` ON DELETE CASCADE, not null |
| committed_points | numeric(6,1) | not null |
| completed_points | numeric(6,1) | not null |

Unique `(sprint_id)`. Index `(project_id)` for the project-level trend view (PRD FR-35).

### 8.3 `cfd_snapshots`
| Column | Type | Constraints |
|---|---|---|
| id | uuid | PK |
| board_id | uuid | FK → `boards.id` ON DELETE CASCADE, not null |
| snapshot_date | date | not null |
| status_name | varchar(64) | not null |
| issue_count | integer | not null |

Unique `(board_id, snapshot_date, status_name)`.

### 8.4 `sprint_summaries`
| Column | Type | Constraints |
|---|---|---|
| sprint_id | uuid | PK, FK → `sprints.id` ON DELETE CASCADE |
| planned_points | numeric(6,1) | not null |
| completed_points | numeric(6,1) | not null |
| scope_added_points | numeric(6,1) | not null, default 0 |
| scope_removed_points | numeric(6,1) | not null, default 0 |
| carry_over_issue_count | integer | not null, default 0 |

---

## 9. Notifications & Background Jobs

### 9.1 `notification_outbox`
| Column | Type | Constraints |
|---|---|---|
| id | uuid | PK |
| recipient_id | uuid | FK → `users.id` ON DELETE CASCADE, not null |
| event_type | varchar(64) | not null (e.g., `ISSUE_ASSIGNED`, `USER_MENTIONED`) |
| payload | jsonb | not null |
| channel | varchar(16) | not null — `IN_APP` \| `EMAIL` |
| status | varchar(16) | not null — `PENDING` \| `SENT` \| `FAILED` \| `DEAD_LETTERED`, default `PENDING` |
| attempt_count | integer | not null, default 0 |
| next_attempt_at | timestamptz | not null, default `now()` |
| created_at | timestamptz | per §2 |

Index `(status, next_attempt_at)` — the exact predicate the polling worker (LLD §8, §10) queries on. Written in the **same transaction** as the triggering domain event's originating change (HLD §8.1 outbox pattern), never as a separate best-effort insert.

### 9.2 `notifications` (in-app, read state)
| Column | Type | Constraints |
|---|---|---|
| id | uuid | PK |
| recipient_id | uuid | FK → `users.id` ON DELETE CASCADE, not null |
| event_type | varchar(64) | not null |
| payload | jsonb | not null |
| read_at | timestamptz | nullable |
| created_at | timestamptz | per §2 |

Index `(recipient_id, read_at)` — unread-count and inbox queries.

### 9.3 `background_jobs`
| Column | Type | Constraints |
|---|---|---|
| id | uuid | PK |
| job_type | varchar(48) | not null — `SPRINT_ROLLOVER` \| `NOTIFICATION_DISPATCH` \| `PROJECTION_REBUILD` \| `REMINDER` (LLD §10) |
| idempotency_key | varchar(255) | not null |
| payload | jsonb | not null |
| status | varchar(16) | not null — `PENDING` \| `IN_PROGRESS` \| `COMPLETED` \| `FAILED`, default `PENDING` |
| attempt_count | integer | not null, default 0 |
| next_run_at | timestamptz | not null, default `now()` |
| created_at / updated_at | timestamptz | per §2 |

Unique `(job_type, idempotency_key)` — this is what makes LLD §10's idempotency guarantee a DB-enforced fact rather than an application convention: a duplicate enqueue attempt fails the insert rather than silently creating a second job. Index `(status, next_run_at)` for the worker poll loop.

---

## 10. Seed Data — `role_permissions`

Concrete values for the matrix LLD §4.1 specified at the model level (PRD FR-13). Read access to backlog/board views is **not** gated by this table — it requires only an active `project_memberships` row of any role; the permissions below govern mutating actions and the reports view specifically.

| Role | CREATE_ISSUE | EDIT_ISSUE | TRANSITION_STATUS | DELETE_ISSUE | MANAGE_SPRINT | CONFIGURE_BOARD | VIEW_REPORTS | MANAGE_PROJECT_USERS |
|---|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|
| ADMIN | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| PROJECT_MANAGER_SCRUM_MASTER | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | — |
| PRODUCT_OWNER | ✓ | ✓ | ✓ | — | — | — | ✓ | — |
| DEVELOPER | ✓ | ✓ | ✓ | — | — | — | ✓ | — |
| QA_TESTER | ✓ | ✓ | ✓ | — | — | — | ✓ | — |
| VIEWER_STAKEHOLDER | — | — | — | — | — | — | ✓ | — |

**Design note — `MANAGE_PROJECT_USERS` is Admin-only, not granted to Project Manager/Scrum Master.** PRD's admin-governed philosophy (§1.1: "nothing... exists unless an administrator explicitly provisions it") is read here as applying to project membership too — only Admin adds/removes users from a project or changes their role. A PM/Scrum Master's PRD-described "team assignment within their project" (§2 persona table) is interpreted as assigning *existing project members* to issues/sprints (covered by `EDIT_ISSUE`/`MANAGE_SPRINT`), not granting them project access — that distinction is called out explicitly here because it's a judgment call on ambiguous PRD wording, not a directly-stated requirement, and should be confirmed at review rather than silently assumed.

Delete-project and archive-project (PRD FR-7, FR-8) are Admin-only operations at the service layer (LLD §4.3) and are not part of this per-issue permission matrix at all — they're governance actions, not project-content actions.

---

## 11. Indexing & Performance Summary

Directly tied to HLD §13 targets:

| Target | Supporting design |
|---|---|
| P95 read < 300 ms at 10k+ issues/project | `(project_id, status)` and `(project_id, sprint_id)` composite indexes on `issues` cover the two dominant board/backlog queries without a sequential scan. |
| Report generation < 1 s | Reporting tables (§8) are pre-aggregated projections, queried by simple indexed lookups (`sprint_id`, `board_id` + date range), never joined against raw `issues` history at read time. |
| Search < 500 ms (v1) | GIN index on `issues.search_vector`; extraction seam to OpenSearch if the HLD §5 200k-issue/multi-facet trigger is hit. |
| Cache hit ratio > 90% | Board/backlog reads are Redis-cached ahead of hitting these indexes at all (HLD §5) — indexes are the fallback path on cache miss, not the primary path. |
| Connection pool sizing | Per HLD §13, finalized against actual instance count; this schema's index set is designed to keep per-query execution time low enough that pool exhaustion is driven by traffic volume, not slow queries holding connections open. |

---

## 12. Out of Scope for This Phase

- REST endpoint design, request/response DTOs → **API Design phase**.
- UI screens/components → **UI Design phase**.
- Physical replication configuration (streaming replication setup, `synchronous_commit` tuning for the HLD §12 standby) — an operational/deployment task, not a schema concern.
- Table partitioning (e.g., partitioning `issues` or `audit_log` by date/project at very large scale) — not needed at the confirmed Large-scale ceiling (~1M issues); flagged as a future revisit if actual production volume exceeds the confirmed target.
- Exact migration file structure/tooling — implementation-phase detail.

---

## 13. Approval & Next Steps

This Database Design is **not final** until:
1. The `role_permissions` design note in §10 (Admin-only `MANAGE_PROJECT_USERS`, excluding PM/Scrum Master) is confirmed or corrected by stakeholders — it resolves an ambiguity in the PRD's persona description rather than a directly-stated requirement.
2. This document is explicitly reviewed and approved.

Upon approval, the next phase is **API Design**: REST endpoint contracts, request/response payloads, pagination/filtering conventions, and the standardized error-body shape for the `DomainException` hierarchy defined in LLD §13 — one endpoint set per module, covering every operation named across the PRD's functional requirements. No implementation code will be written before API Design and the subsequent UI Design phase are reviewed in turn.

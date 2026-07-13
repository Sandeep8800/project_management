# Nexus PMS — Low-Level Design (LLD)

**Status:** Draft for review
**Phase:** 3 of 7 — Documentation-first sequence (PRD → HLD → **LLD** → Database Design → API Design → UI Design → Coding)
**Input:** [`01-PRD.md`](./01-PRD.md) (Approved) · [`02-HLD.md`](./02-HLD.md) (Approved)
**Owners:** Architecture / Engineering
**Last updated:** 2026-07-13

> This document translates the HLD's six modules into concrete internal design: service responsibilities, state machines, the permission model, the domain event catalog, and key transaction/sequence flows. It does **not** define database DDL/indexes (Database Design phase), REST contracts/payloads (API Design phase), or UI components (UI Design phase). No implementation code is written against this phase.

---

## 1. Purpose & Scope

For each HLD module (HLD §4), define:
- Internal service decomposition and responsibilities.
- The state machines that govern issue workflow and sprint lifecycle (PRD FR-23, FR-25, FR-30).
- The permission-matrix data model backing API-layer RBAC (HLD §6, §7).
- The domain event catalog that HLD §8.6's `DomainEventPublisher` abstraction carries, and which module consumes what.
- Concurrency/consistency rules and key end-to-end sequence flows for the operations most likely to have race conditions or cross-module effects.

---

## 2. Package & Layering Conventions

Package-by-feature (one top-level package per HLD module), each internally layered the same way, so the modular-monolith boundary (HLD §2) is enforced by convention and, where practical, by build-time module boundaries (e.g., Java module-info or ArchUnit rules preventing cross-module package reach-ins):

```
com.nexuspms.<module>
 ├── api/          controller layer — thin, delegates to service layer only
 ├── service/       use-case orchestration, transaction boundaries live here
 ├── domain/        entities, value objects, state machines, domain events
 ├── repository/    persistence port (interface) + JPA implementation
 └── event/         published/consumed domain event types for this module
```

**Rule:** a module's `repository` and `domain` packages are never imported by another module. Cross-module reads happen through the owning module's `service` interface; cross-module reactions happen through domain events (§6). This is what makes the §2 HLD extraction escape hatch real rather than aspirational — if a module never got direct repository access from outside, extracting it later is a deployment change, not a data-access rewrite.

---

## 3. Module: Identity & Access

**Services:**
- `AuthenticationService` — validates local credentials (bcrypt) or delegates to `SsoAuthenticationService` for SAML/OIDC callbacks; both paths converge on issuing an internal JWT (HLD §7).
- `SsoLinkingService` — implements the auto-link-by-email rule (PRD FR-4, HLD §7): on a verified SSO assertion, look up an existing `User` by exact email; link the SSO subject to it if unlinked; reject the login (never create an account) if no match exists.
- `UserAdminService` — admin-only operations: create/deactivate/reactivate user, trigger password reset / SSO provisioning (PRD FR-1–5). Every mutating call here is itself a Governance & RBAC audit event (§6).
- `TokenService` — JWT issuance, refresh-token rotation, revocation-list management (backed by the Redis session cache, HLD §5).

**Domain concepts (conceptual, not DDL):** `User` (identity, status: ACTIVE/DEACTIVATED), `LocalCredential`, `SsoIdentityLink` (one-to-one per IdP per user), `RefreshToken`.

**Deactivation semantics (PRD FR-5):** deactivation is a status flag, not a delete — `UserAdminService.deactivate()` additionally triggers removal from active project-membership assignment pools (delegated call into Governance & RBAC's `ProjectMembershipService`) while leaving historical attribution (past comments, resolved issues, audit entries) untouched, since those reference the user by immutable ID, not by active-status.

---

## 4. Module: Governance & RBAC

This is the module every other module's write path depends on for authorization, so its internal design is the most load-bearing part of the LLD.

### 4.1 Permission Matrix Data Model

- `Role` — the fixed enum set from PRD §3.1.3: `ADMIN`, `PROJECT_MANAGER_SCRUM_MASTER`, `PRODUCT_OWNER`, `DEVELOPER`, `QA_TESTER`, `VIEWER_STAKEHOLDER`. **Not admin-editable in v1** — PRD does not request custom roles, only a fixed role set with a permission matrix (FR-13), so the role set itself is a code-level enum, not an admin-managed table. This is called out explicitly because it constrains Database Design (roles are a reference/lookup concept, not a user-editable entity).
- `Permission` — enum of the actions named in PRD FR-13: `CREATE_ISSUE`, `EDIT_ISSUE`, `TRANSITION_STATUS`, `DELETE_ISSUE`, `MANAGE_SPRINT`, `CONFIGURE_BOARD`, `VIEW_REPORTS`, `MANAGE_PROJECT_USERS`, plus any additional actions identified during API Design as endpoints are enumerated.
- `RolePermission` — the matrix itself: a seeded, read-only-at-runtime mapping of `Role → Set<Permission>`. Seeded at deployment (config or migration-seeded reference data), **visible to Admin via the dashboard (PRD FR-16) but not editable through the UI in v1** — editable permission schemes per project (Jira-style custom permission schemes) were explicitly out of PRD scope (HLD §16) and are not designed here.
- `ProjectMembership` — the actual per-user, per-project assignment: `(user_id, project_id, role)`, one row per user per project (PRD FR-11). This is the entity that makes roles project-scoped rather than global.

### 4.2 Permission Resolution

`PermissionResolver.resolve(userId, projectId) → Set<Permission>`:
1. Look up `ProjectMembership` for `(userId, projectId)`. Admin platform-wide role (HLD §1) short-circuits to the full permission set regardless of per-project membership.
2. Map the membership's `Role` through the static `RolePermission` matrix.
3. Result is cached in Redis under `(userId, projectId)` (HLD §5) with invalidation triggered by `ProjectMembershipChanged` domain events (§6) — so a role change takes effect on the next request, not after a TTL expiry.

`AuthorizationInterceptor` (HLD §6) calls `PermissionResolver` and checks the requested `Permission` against the resolved set before any controller method body executes. This is a single call site — every module's controllers rely on the same interceptor rather than each implementing its own check, which is what makes "server is the source of truth" structurally true rather than a convention that could be forgotten in one module.

### 4.3 Project Lifecycle

`ProjectAdminService` — create/archive/delete (PRD FR-6–9). Delete requires the archive-first safeguard (FR-8): `deleteProject()` throws `IllegalStateException`-style domain error unless the project is already `ARCHIVED`, enforced in the service layer (not just a UI guard). Project key immutability (FR-9) is enforced by rejecting key-edit requests once `Issue.count(projectId) > 0`.

### 4.4 Audit

`AuditService.record(actor, actionType, targetEntity, timestamp)` — called synchronously, in the same transaction, by every Governance & RBAC mutating method (user create/deactivate, project create/archive/delete, role assignment change — PRD FR-15). Not implemented as an AOP-only cross-cutting concern that could silently no-op; each admin service method explicitly calls it, so a missing audit call is a visible code-review defect, not a silent runtime gap. Query side (`AuditQueryService`) supports the filters required by FR-17 (actor, action type, date range, target entity).

---

## 5. Module: Backlog & Issues

**Services:**
- `IssueService` — create/edit/delete issues across all five types (Epic/Story/Task/Sub-task/Bug, PRD FR-18), enforcing the parent/child hierarchy (FR-20) at the service layer (e.g., a Sub-task must reference a Task or Story parent; a Bug's parent link is optional).
- `IssueLinkService` — blocks/is-blocked-by/relates-to/duplicates links (FR-31), modeled as a directed, typed edge between two issues.
- `CommentService` — comments with @mention parsing (FR-33); mention extraction publishes `UserMentioned` domain events (§6) consumed by Notifications.
- `BacklogService` — ordering/prioritization within a project's single backlog (FR-19, and the HLD §4 shared-backlog decision) — backlog order is a per-project ordinal on `Issue`, not a separate backlog entity, since HLD resolved that Scrum/Kanban boards are views over one backlog, not separate tracks.

**Concurrency:** `Issue` carries an optimistic-lock version. Two users editing the same issue concurrently (e.g., both changing assignee) — the second write fails with a `ConcurrentModificationException`-style domain error surfaced to the client as a conflict (exact HTTP mapping at API Design), rather than silently last-write-wins, since silent overwrites on a shared backlog are a real risk at the Large-scale/100+ project target.

**Attachments:** `AttachmentService` stores only metadata in the primary datastore; binary content goes to object storage (HLD §5) via a pre-signed upload URL pattern (detail at API Design) — the service never proxies file bytes through the app tier.

---

## 6. Module: Sprint & Board (incl. Workflow Engine)

### 6.1 Sprint State Machine (PRD FR-21–25, HLD resolved: one active sprint per project in v1)

```
   create()
      │
      ▼
  ┌────────┐   start()    ┌────────┐   complete()   ┌───────────┐
  │ PLANNED │ ───────────► │ ACTIVE  │ ─────────────► │ COMPLETED  │
  └────────┘               └────────┘                └───────────┘
```

- `SprintService.start(sprintId)` — guarded by a check that no other sprint for the same project is currently `ACTIVE` (enforces the v1 single-active-sprint decision, HLD §1); locks sprint start/end dates; transitions state; publishes `SprintStarted`.
- `SprintService.complete(sprintId, rolloverDecision)` — `rolloverDecision` is an explicit parameter (`MOVE_TO_BACKLOG` or `MOVE_TO_NEXT_SPRINT` with a target sprint ID), never a silent default, per PRD FR-25. Incomplete issues are re-parented accordingly in the same transaction; publishes `SprintCompleted`, which is what triggers the Reporting module's sprint-summary projection finalization (§7) and the background-job-driven rollover processing for large sprints (HLD §8.5 — the rollover *decision* is synchronous and immediate, but bulk re-parenting of many issues is offloaded to the background job queue so `complete()` doesn't block the request thread on a large sprint).
- A `PLANNED` sprint has no board presence; only `ACTIVE` sprints populate the Scrum board (HLD §4).

### 6.2 Workflow Engine (PRD FR-30)

Per HLD §4, this is a sub-component of Sprint & Board, not a top-level module in v1.

- `WorkflowDefinition` — per-project, a directed graph of `Status` nodes and allowed `Transition` edges. Ships with a default graph (`To Do → In Progress → In Review → Done`) that a project can customize (add/rename/reorder statuses, redefine allowed transitions) per FR-30.
- `WorkflowTransitionValidator.validate(issue, targetStatus, projectWorkflow) → allow/deny` — checked **after** `AuthorizationInterceptor`'s RBAC check (§4.2), never before: "is this user allowed to transition issues" and "is this specific transition legal in this project's workflow" are independent questions, and RBAC failing should short-circuit before workflow evaluation runs.
- Board columns (HLD FR-26/FR-27/FR-29) are a per-board *view* mapping of one-or-more workflow statuses to a column — not a redefinition of the workflow itself, so Scrum and Kanban boards for the same project can present the same underlying statuses differently (e.g., collapse "In Review" and "QA Review" into one Kanban column) without diverging the actual status data (consistent with the shared-backlog decision).

### 6.3 Board Service

`BoardService` — resolves the current board state (issues grouped by column) for either board type; Kanban boards additionally enforce configurable per-column WIP limits (FR-26) at the point an issue is moved into a column, rejecting the move (surfaced to the client, not silently allowed) if it would exceed the limit.

---

## 7. Module: Reporting

Per HLD §5/§8.2, all reports are served from projections, never live-aggregated.

- `ProjectionUpdateService` — the sole consumer that reacts to `IssueStatusChanged`, `IssueCreated`, `SprintStarted`, `SprintCompleted` events (§6) and incrementally updates three projection stores:
  - `BurndownSnapshot` (per sprint, per day: remaining scope) — FR-34.
  - `VelocityDataPoint` (per completed sprint: committed vs. completed points) — FR-35.
  - `CfdSnapshot` (per board, per day: issue count per status) — FR-36.
- `SprintSummaryService` — composes a sprint's planned-vs-completed and scope-change data at `SprintCompleted` time (FR-37), rather than recomputing on every report view.
- `ReportQueryService` — pure read path against replicas (HLD §5), no write access — reinforces that reports can never mutate state, keeping the Viewer/Stakeholder read-only guarantee (FR-38) architecturally true, not just RBAC-enforced.
- **Backfill/repair:** `ProjectionRebuildJob` (a background job type, HLD §8.5) can recompute a projection from raw issue/sprint history for a single project — used for correcting drift or onboarding historical data, not on the request path.

---

## 8. Module: Notifications

- `NotificationDispatchService` — consumes events (assignment change, `UserMentioned`, `IssueStatusChanged` on watched/assigned issues, `SprintStarted`, `SprintCompleted` — PRD FR-39) via the domain event bus (§6) and writes an outbox row (HLD §8.1) in the same transaction as the triggering change.
- Delivery is a strategy per channel: `InAppChannel` (always on) and `EmailChannel` (per-user opt-in, HLD §8.1) — both implement a common `NotificationChannel` interface so adding a channel later doesn't change `NotificationDispatchService`.
- A background job (HLD §8.5) polls the outbox and dispatches; failed email sends retry with backoff up to a bounded attempt count, then land in a dead-letter state visible to Admin (operational visibility, not a PRD-required feature, but necessary so silently-failed notifications are discoverable).
- Real-time push (HLD §8.4) is a separate consumer of the same events — `NotificationDispatchService` does not call the WebSocket layer directly; both subscribe to the domain event bus independently, keeping the two concerns decoupled.

---

## 9. Domain Event Catalog

The concrete instantiation of HLD §8.6's `DomainEventPublisher` abstraction. Every module publishes only these events; consumers subscribe by event type, never by calling the publishing module's internals directly.

| Event | Published by | Consumed by |
|---|---|---|
| `UserCreated` / `UserDeactivated` / `UserReactivated` | Identity & Access | Governance & RBAC (audit) |
| `ProjectCreated` / `ProjectArchived` / `ProjectDeleted` | Governance & RBAC | Reporting (projection lifecycle), Audit |
| `ProjectMembershipChanged` | Governance & RBAC | Governance & RBAC itself (permission cache invalidation, §4.2), Notifications |
| `IssueCreated` / `IssueStatusChanged` / `IssueAssigned` | Backlog & Issues | Reporting, Notifications, Real-Time push (HLD §8.4), Sprint & Board (board state) |
| `UserMentioned` | Backlog & Issues (Comments) | Notifications |
| `SprintStarted` / `SprintCompleted` | Sprint & Board | Reporting, Notifications |
| `NotificationDispatchFailed` | Notifications | Notifications itself (dead-letter handling), Observability/alerting |

This table is the seed for the API Design phase's eventual webhook/integration extension point noted in HLD §16 — not built now, but the event catalog is what a future Integration module would subscribe to without any change to the publishing modules.

---

## 10. Background Job Catalog

Concrete job types for HLD §8.5's durable job table:

| Job Type | Triggered by | Idempotency key |
|---|---|---|
| `SprintRolloverJob` | `SprintService.complete()` for sprints above a bulk-size threshold | `sprintId` (re-running re-checks each issue's current sprint before re-parenting) |
| `NotificationDispatchJob` | Outbox row insert (§8) | outbox row ID |
| `ProjectionRebuildJob` | Admin-triggered (drift repair) or scheduled integrity check | `(projectId, projectionType)` |
| `ReminderJob` | Scheduled (sprint start/end reminders, FR-39) | `(sprintId, reminderType)` |

---

## 11. Key Sequence Flows

### 11.1 Issue Status Transition
1. Client (Developer) requests transition on an issue's board.
2. `AuthorizationInterceptor` resolves permissions for `(user, project)`; denies if `TRANSITION_STATUS` isn't in the resolved set (§4.2).
3. `WorkflowTransitionValidator` checks the target status is a legal transition from the issue's current status per the project's `WorkflowDefinition` (§6.2); denies otherwise.
4. `IssueService` applies the change under optimistic locking (§5); on version conflict, returns a conflict error rather than overwriting.
5. `IssueStatusChanged` published.
6. Consumers react independently and in parallel: Reporting updates projections; Notifications dispatches to watchers; Real-Time push notifies subscribed board clients (HLD §8.4); Sprint & Board's own board-state cache (HLD §5 hot-read cache) is invalidated.

### 11.2 Sprint Completion with Rollover
1. PM/Scrum Master calls `SprintService.complete(sprintId, rolloverDecision)`.
2. Sprint state transitions to `COMPLETED` synchronously; incomplete-issue count checked.
3. If incomplete-issue count is small, re-parenting happens inline in the same transaction; if above the bulk threshold, a `SprintRolloverJob` is enqueued (§10) and the sprint transition still completes immediately — the PM isn't blocked waiting on bulk issue updates.
4. `SprintCompleted` published, triggering `SprintSummaryService` (§7) and reminder/notification consumers.

### 11.3 Project Role Assignment Change
1. Admin calls `ProjectMembershipService.assignRole(userId, projectId, role)` (Governance & RBAC).
2. Written transactionally alongside `AuditService.record(...)` (§4.4) — both succeed or both fail together.
3. `ProjectMembershipChanged` published; `PermissionResolver`'s Redis cache entry for `(userId, projectId)` is invalidated (§4.2) so the next request re-resolves against the new role, not a stale cached one.

### 11.4 SSO Login with Auto-Link
1. User authenticates against the enterprise IdP; SAML/OIDC assertion returned to `SsoAuthenticationService`.
2. `SsoLinkingService` looks up `User` by exact email match from the assertion.
3. Match found + unlinked → link `SsoIdentityLink`, issue JWT. Match found + already linked to a different IdP subject → reject (prevents identity confusion). No match → reject login entirely; **no account is created** (PRD FR-4, HLD §7).

---

## 12. Concurrency & Consistency Summary

| Scenario | Mechanism |
|---|---|
| Concurrent issue edits (e.g., two users reassign the same issue) | Optimistic locking (version column), conflict surfaced to client (§5) |
| Concurrent sprint start attempts for the same project | `SprintService.start()` transaction checks for an existing `ACTIVE` sprint under row-level lock on the project's sprint set, second caller gets a domain conflict error |
| Role change mid-request | Permission cache invalidation is synchronous with the audit-logged change (§11.3); a request already past the `AuthorizationInterceptor` check for that call completes under the old permission (acceptable — not retroactively revoked mid-request) |
| Bulk sprint rollover | Offloaded to background job (§11.2), idempotent by `sprintId` so a worker crash and retry doesn't double-move issues |

---

## 13. Error Handling Strategy

A shared `DomainException` hierarchy, one exception type per failure category, mapped to HTTP responses centrally at the API layer (exact status codes/error body shape defined in API Design, not here):
- `AuthorizationDeniedException` (RBAC denial, §4.2)
- `InvalidWorkflowTransitionException` (§6.2)
- `ResourceNotFoundException`
- `ConcurrentModificationException` (§5, §12)
- `GovernanceSafeguardException` (e.g., delete-without-archive-first, §4.3)

No module throws raw/unchecked exceptions across its service boundary — every public service method's failure modes are one of the above, which is what keeps the eventual API error contract (API Design phase) consistent across all six modules instead of ad hoc per controller.

---

## 14. Testing Strategy (Outline)

- **Unit tests** per service, especially `PermissionResolver`, `WorkflowTransitionValidator`, and the `SprintService` state machine — these carry the most business-rule risk.
- **Integration tests** per module boundary: verify a module never queries another module's repository directly (enforced additionally by an ArchUnit-style static check, §2).
- **Event-contract tests**: for each row in the §9 catalog, a test asserting the publishing module actually emits the event under the documented trigger, and each consumer handles it — guards against silent drift between this document and the implementation.
- **Load testing**: validated against the HLD §13 NFR targets (P95 latency, board load time, WebSocket concurrency) before the Database Design phase's indexing choices are treated as final — surfaces whether the projection/caching design here actually holds at the Large-scale target, not just in theory.

---

## 15. Out of Scope for This Phase

- Database DDL, table/column definitions, indexes, constraints → **Database Design phase**.
- REST endpoint paths, request/response payloads, HTTP status/error body shape → **API Design phase**.
- Screen layouts, component inventory → **UI Design phase**.
- Exact seed data for the `RolePermission` matrix (the mapping is designed here at the model level, §4.1; the literal seed values are a Database Design deliverable).
- Capabilities listed in HLD §16 (automation, plugins, integrations, enterprise modules) — not designed here, consistent with the PRD scope decision.

---

## 16. Approval & Next Steps

This LLD is **not final** until explicitly reviewed and approved. Upon approval, the next phase is **Database Design**: concrete schema (tables, columns, types, constraints, indexes) for every entity named here — `User`, `ProjectMembership`, `Issue` (and its type hierarchy), `WorkflowDefinition`/`Transition`, `Sprint`, the `RolePermission` seed data, projection tables, the outbox/job tables — sized and indexed against the HLD §13 NFR targets and the Large-scale target (HLD §1). No implementation code will be written before Database Design and the subsequent API Design and UI Design phases are each reviewed in turn.

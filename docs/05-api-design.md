# Nexus PMS — API Design

**Status:** Draft for review
**Phase:** 5 of 7 — Documentation-first sequence (PRD → HLD → LLD → Database Design → **API Design** → UI Design → Coding)
**Input:** [`01-PRD.md`](./01-PRD.md) · [`02-HLD.md`](./02-HLD.md) · [`03-LLD.md`](./03-LLD.md) · [`04-database-design.md`](./04-database-design.md) (all Approved)
**Owners:** Architecture / API
**Last updated:** 2026-07-13

> This document defines REST endpoint contracts: resource paths, methods, request/response shapes, pagination/filtering conventions, and the standardized error model. It does not define UI screens (UI Design phase) or implementation code. Every endpoint below maps to an LLD service method and a Database Design table; nothing here introduces new behavior not already approved in prior phases.

---

## 1. Purpose & Scope

Specify the concrete HTTP contract for every operation named across PRD §3 (Admin, Backlog, Sprint, Board, Reporting, Notifications), built on:
- The module boundaries and `AuthorizationInterceptor` enforcement point from HLD §4, §6.
- The service methods and `DomainException` hierarchy from LLD §3–§13.
- The schema from Database Design §4–§9.

**Style:** RESTful JSON, versioned at `/api/v1`, resource-oriented per module — decided and justified in HLD §6 (REST over GraphQL, for RBAC auditability). This phase fills in the contract detail HLD deferred.

---

## 2. Conventions

### 2.1 Base URL & Auth
- Base path: `/api/v1`
- Every request (except `/auth/login`, `/auth/sso/**`, `/auth/refresh`) requires `Authorization: Bearer <JWT>` (HLD §6). Missing/invalid/expired token → `401 UNAUTHENTICATED` (§3).
- Every request beyond authentication passes through `AuthorizationInterceptor` (HLD §6, LLD §4.2) before the controller body runs — the required permission is documented per endpoint below (§5–§10). A caller authenticated but lacking the permission gets `403` (§3), never a silently-filtered/empty result.

### 2.2 Pagination
Two styles, chosen per access pattern (Database Design §11):
- **Offset pagination** (`?page=0&size=20`, max `size=100`) for admin lists (users, projects, memberships) and audit log — bounded, low-cardinality lists where deep paging is rare.
- **Keyset/cursor pagination** (`?cursor=<opaque>&limit=50`, max `limit=100`) for `issues`/backlog listing — avoids the `OFFSET`-scan cost at 10k+ issues/project (Database Design §6.1, HLD §13 latency target).

List response envelope (both styles):
```json
{
  "data": [ /* resource objects */ ],
  "pagination": {
    "nextCursor": "eyJpZCI6Ii4uLiJ9",   // cursor style; omitted for offset style
    "page": 0, "size": 20, "totalElements": 137,  // offset style; omitted for cursor style
    "hasMore": true
  }
}
```

### 2.3 Filtering & Sorting
Query parameters per resource, documented per endpoint. Common pattern for `issues`: `?status=&assignee=&sprintId=&type=&label=&q=` (full-text, backed by `search_vector`, Database Design §6.1) `&sort=priority,desc`.

### 2.4 Concurrency Control
Mutating requests against optimistically-locked resources (`issues`, `sprints` — Database Design §2, §6.1, §7.1) require an `If-Match: "<version>"` header carrying the version last read by the client. A stale version → `409 CONCURRENT_MODIFICATION` (§3), never a silent overwrite (LLD §5, §12).

### 2.5 Idempotency
`POST` endpoints that trigger a background job (sprint completion with bulk rollover, LLD §11.2) are idempotent by the underlying job's `(job_type, idempotency_key)` DB constraint (Database Design §9.3) — a retried request against an already-enqueued rollover does not double-enqueue.

---

## 3. Standard Error Model

Every non-2xx response uses one shape, mapping the LLD §13 `DomainException` hierarchy plus standard auth/validation failures:

```json
{
  "error": {
    "code": "INVALID_WORKFLOW_TRANSITION",
    "message": "Cannot transition issue NEX-142 from 'Done' to 'In Progress' under the project workflow.",
    "details": { "issueId": "...", "fromStatus": "Done", "toStatus": "In Progress" },
    "traceId": "3fa1c2e0-..."
  }
}
```

| HTTP Status | `code` | Source |
|---|---|---|
| 400 | `VALIDATION_ERROR` | Request body/param failed schema validation (not a domain exception — bean-validation layer) |
| 401 | `UNAUTHENTICATED` | Missing/invalid/expired JWT |
| 403 | `AUTHORIZATION_DENIED` | LLD `AuthorizationDeniedException` — caller lacks the required permission on the target project |
| 404 | `RESOURCE_NOT_FOUND` | LLD `ResourceNotFoundException` |
| 409 | `CONCURRENT_MODIFICATION` | LLD `ConcurrentModificationException` — stale `If-Match` version |
| 409 | `SPRINT_ALREADY_ACTIVE` | DB partial-unique-index violation surfaced from `SprintService.start()` (Database Design §7.1) |
| 409 | `GOVERNANCE_SAFEGUARD` | LLD `GovernanceSafeguardException` — e.g., delete-project without archive-first (PRD FR-8) |
| 422 | `INVALID_WORKFLOW_TRANSITION` | LLD `InvalidWorkflowTransitionException` — legal permission, illegal workflow transition |
| 500 | `INTERNAL_ERROR` | Unhandled — logged with `traceId` for correlation (HLD §11) |

`traceId` on every error response ties back to the HLD §11 correlation-ID/tracing design — this is the field support/on-call uses to find the matching server-side trace.

---

## 4. Auth Endpoints (Identity & Access)

| Method | Path | Required Permission | Description |
|---|---|---|---|
| POST | `/auth/login` | none (unauthenticated) | Local credential login. Body: `{ "email", "password" }`. Returns `{ "accessToken", "refreshToken" }`. |
| GET | `/auth/sso/{provider}/login` | none | Redirects into the enterprise IdP's SAML/OIDC flow (LLD §3, §11.4). |
| GET/POST | `/auth/sso/{provider}/callback` | none | IdP callback; on success runs `SsoLinkingService` auto-link-by-email and returns tokens, or `403 SSO_NO_MATCHING_ACCOUNT` if no admin-provisioned account matches (never creates one). |
| POST | `/auth/refresh` | none (valid refresh token) | Body: `{ "refreshToken" }` → rotated `{ "accessToken", "refreshToken" }`. |
| POST | `/auth/logout` | authenticated | Revokes the current refresh token. |

## 5. Admin Endpoints (Identity & Access + Governance & RBAC)

All under `/admin`, all requiring platform-wide Admin (HLD §1) — not a project-scoped permission, since these are governance actions (PRD §3.1).

| Method | Path | Description |
|---|---|---|
| POST | `/admin/users` | Create user (PRD FR-1). Body: `{ name, email, employeeId, department, defaultRole }`. |
| GET | `/admin/users` | List users. Filters: `?status=&department=&q=`. Offset-paginated. |
| GET | `/admin/users/{userId}` | User detail, including all `project_memberships` (PRD FR-14 per-user view). |
| PATCH | `/admin/users/{userId}` | Update name/department/defaultRole. |
| POST | `/admin/users/{userId}/deactivate` | PRD FR-2. |
| POST | `/admin/users/{userId}/reactivate` | PRD FR-3. |
| POST | `/admin/users/{userId}/reset-password` | Triggers local password reset (PRD FR-4). |
| POST | `/admin/users/{userId}/provision-sso` | Marks the account SSO-eligible; does not create an `sso_identity_links` row itself (that happens at first successful login, LLD §11.4). |
| POST | `/admin/projects` | Create project (PRD FR-6). Body: `{ projectKey, name, description, methodology, startDate, targetReleaseDate }`. |
| GET | `/admin/projects` | List all projects (admin view, incl. archived). Filters: `?status=&methodology=`. |
| POST | `/admin/projects/{projectId}/archive` | PRD FR-7. |
| DELETE | `/admin/projects/{projectId}` | PRD FR-8 — `409 GOVERNANCE_SAFEGUARD` unless already `ARCHIVED`. |
| GET | `/admin/projects/{projectId}/memberships` | Per-project membership view (PRD FR-14). |
| POST | `/admin/projects/{projectId}/memberships` | Assign a user + role to the project (PRD FR-10, FR-11). Body: `{ userId, role }`. `409` if the `(user_id, project_id)` pair already exists — use `PATCH` to change role. |
| PATCH | `/admin/projects/{projectId}/memberships/{userId}` | Change a user's role on this project. Body: `{ role }`. |
| DELETE | `/admin/projects/{projectId}/memberships/{userId}` | Remove a user from the project. |
| GET | `/admin/dashboard` | Consolidated projects/users/role-assignment view (PRD FR-16). |
| GET | `/admin/audit-log` | Query the append-only log (PRD FR-17). Filters: `?actorId=&actionType=&targetEntityType=&targetEntityId=&from=&to=`. Offset-paginated. |

Every write above triggers a synchronous `audit_log` insert in the same transaction (LLD §4.4, Database Design §5.4) — not separately documented per row here since it's structural, not optional per-endpoint behavior.

---

## 6. Backlog & Issues Endpoints

Base: `/projects/{projectId}/...` or `/issues/{issueId}` once the issue is addressed directly. Required permission per PRD's role matrix (Database Design §10).

| Method | Path | Required Permission | Description |
|---|---|---|---|
| POST | `/projects/{projectId}/issues` | `CREATE_ISSUE` | Body: `{ issueType, title, description, priority, parentIssueId?, storyPoints?, assigneeId?, labels? }`. Server assigns `issueKey` (`{projectKey}-{seq}`). |
| GET | `/projects/{projectId}/issues` | membership (any role, read-only per Database Design §10 note) | Cursor-paginated. Filters: `status, assignee, sprintId, type, label, q`. Backs backlog and board views. |
| GET | `/issues/{issueId}` | membership | Full issue detail incl. links, comments (paginated separately), attachments. |
| PATCH | `/issues/{issueId}` | `EDIT_ISSUE` + `If-Match` version header | Partial update (title, description, priority, assignee, storyPoints, labels). `409 CONCURRENT_MODIFICATION` on stale version. |
| DELETE | `/issues/{issueId}` | `DELETE_ISSUE` | Hard delete — issue history concerns are handled at project level (archive), not issue level; PRD does not require issue-level soft delete. |
| POST | `/issues/{issueId}/transitions` | `TRANSITION_STATUS` (checked first) then workflow validity (checked second, LLD §6.2, §11.1) | Body: `{ targetStatus }`. `422 INVALID_WORKFLOW_TRANSITION` if the target isn't reachable from the current status in the project's `workflow_transitions`. |
| PATCH | `/projects/{projectId}/backlog/reorder` | `EDIT_ISSUE` | Body: `{ issueId, afterIssueId }` (or `beforeIssueId`) — recomputes `backlog_rank` (Database Design §6.1) without a full renumber. |
| POST | `/issues/{issueId}/links` | `EDIT_ISSUE` | Body: `{ targetIssueId, linkType }`. |
| DELETE | `/issue-links/{linkId}` | `EDIT_ISSUE` | |
| GET | `/issues/{issueId}/comments` | membership | Offset-paginated, chronological. |
| POST | `/issues/{issueId}/comments` | membership (commenting is not gated by the mutating-action matrix — any project member can comment, consistent with PRD's collaborative intent; @mentions parsed server-side, LLD §5) | Body: `{ body }`. |
| POST | `/issues/{issueId}/attachments/upload-url` | membership | Returns a pre-signed object-storage upload URL (LLD §5) — the app tier never proxies file bytes. |
| POST | `/issues/{issueId}/attachments` | membership | Registers metadata after direct client→object-storage upload completes. Body: `{ fileName, contentType, sizeBytes, storageKey }`. `400` if `sizeBytes` exceeds the 25 MB limit (HLD §13, Database Design §6.4). |
| GET / POST / DELETE | `/projects/{projectId}/labels` | `CONFIGURE_BOARD` for create/delete, membership for read | Project label management. |

---

## 7. Sprint & Board Endpoints (incl. Workflow Engine)

| Method | Path | Required Permission | Description |
|---|---|---|---|
| POST | `/projects/{projectId}/sprints` | `MANAGE_SPRINT` | Body: `{ name, goal? }`. Created `PLANNED` (LLD §6.1) — no dates required yet. |
| GET | `/projects/{projectId}/sprints` | membership | Filters: `?status=`. |
| PATCH | `/sprints/{sprintId}` | `MANAGE_SPRINT`, `If-Match` | Edit name/goal — only while `PLANNED`. |
| POST | `/sprints/{sprintId}/start` | `MANAGE_SPRINT` | Body: `{ startDate, endDate }`. `409 SPRINT_ALREADY_ACTIVE` if the project already has an active sprint (Database Design §7.1 partial unique index). |
| POST | `/sprints/{sprintId}/complete` | `MANAGE_SPRINT` | Body: `{ rolloverDecision: "MOVE_TO_BACKLOG" \| "MOVE_TO_NEXT_SPRINT", targetSprintId? }` — required, no silent default (PRD FR-25, LLD §6.1). Large sprints enqueue a `SPRINT_ROLLOVER` background job (LLD §11.2); response includes `{ "rolloverJobId": "..." }` when async. |
| GET | `/projects/{projectId}/boards/{boardType}` | membership | `boardType` = `SCRUM` \| `KANBAN`. Returns columns with their issues (Scrum: active sprint's issues; Kanban: full backlog by status, HLD §4 shared-backlog view). Served from the Redis hot-read cache (HLD §5) on hit. |
| PATCH | `/projects/{projectId}/boards/{boardType}/columns` | `CONFIGURE_BOARD` | Reconfigure column set, ordering, `wipLimit` per column (Kanban only, PRD FR-26). |
| GET | `/projects/{projectId}/workflow` | membership | Current `workflow_statuses` + `workflow_transitions` graph (PRD FR-30). |
| PATCH | `/projects/{projectId}/workflow` | `CONFIGURE_BOARD` | Add/rename/reorder statuses, redefine allowed transitions. Does not retroactively invalidate issues already in a removed status (Database Design §7.2 design note). |

---

## 8. Reporting Endpoints (Read-Only)

All served from the projection tables (Database Design §8), replica reads (HLD §5) — no write verbs in this module at all, structurally enforcing the Viewer/Stakeholder read-only guarantee (PRD FR-38).

| Method | Path | Required Permission | Description |
|---|---|---|---|
| GET | `/sprints/{sprintId}/reports/burndown` | `VIEW_REPORTS` | Daily remaining-scope series (PRD FR-34). |
| GET | `/projects/{projectId}/reports/velocity` | `VIEW_REPORTS` | Per-sprint committed vs. completed points, trended (PRD FR-35). Filters: `?lastNSprints=`. |
| GET | `/projects/{projectId}/boards/{boardType}/reports/cfd` | `VIEW_REPORTS` | Cumulative flow diagram data. Query: `?from=&to=` (PRD FR-36). |
| GET | `/sprints/{sprintId}/reports/summary` | `VIEW_REPORTS` | Planned vs. completed, scope changes, carry-over (PRD FR-37). |

---

## 9. Notification Endpoints

| Method | Path | Description |
|---|---|---|
| GET | `/notifications` | Caller's own in-app notifications. Filters: `?unreadOnly=true`. Offset-paginated. |
| POST | `/notifications/{id}/read` | Mark one read. |
| POST | `/notifications/read-all` | Mark all read. |
| GET | `/notifications/preferences` | Current channel opt-ins (in-app always on; email toggle — HLD §8.1). |
| PATCH | `/notifications/preferences` | Body: `{ emailEnabled: boolean }`. |

No project-scoped permission on this module — notifications are always scoped to the authenticated caller as recipient, never queried on another user's behalf through this API.

---

## 10. Real-Time (WebSocket)

Per HLD §8.4, not a REST resource but documented here as it's part of the API contract client applications integrate against.

- **Endpoint:** `wss://.../ws` (STOMP over WebSocket; SSE fallback at `GET /events/stream` with the same destinations expressed as `Last-Event-ID`-resumable streams for environments where WebSocket is blocked).
- **Handshake auth:** JWT passed as a `Authorization` header on the STOMP `CONNECT` frame (not a query-string token, to avoid leaking it into proxy/access logs) — validated identically to a REST request.
- **Subscribe destinations:**
  - `/topic/projects/{projectId}/boards/{boardType}` — board-state change events. Subscribing is authorized exactly like `GET /projects/{projectId}/boards/{boardType}` (HLD §8.4) — a `403`-equivalent STOMP `ERROR` frame closes the subscription attempt if the caller lacks project membership.
  - `/user/queue/notifications` — the caller's own real-time in-app notifications (implicitly scoped to the authenticated principal, no separate authorization check needed).
- **Payload shape** mirrors the relevant REST resource (e.g., a board-topic message payload is the same issue-summary shape returned by `GET /projects/{projectId}/boards/{boardType}`), so clients don't need a second deserialization model.

---

## 11. Cross-Cutting: RBAC Enforcement Summary

Restating HLD §6/§7's structural guarantee in API terms: every endpoint above that lists a `Required Permission` other than bare "membership" is checked against the `role_permissions` matrix (Database Design §10) by `AuthorizationInterceptor` before the handler runs. Endpoints marked "membership" still require an active `project_memberships` row (any role) — an authenticated user with zero relationship to a project gets `403`/`404` (project existence is not distinguishable from lack-of-access to a non-member, to avoid leaking project existence to non-members — a judgment call worth flagging for confirmation rather than silently deciding between `403` and `404` for that specific case).

---

## 12. Out of Scope for This Phase

- UI screens, component-level data-fetching patterns → **UI Design phase**.
- OpenAPI/Swagger document generation and client SDK generation — an implementation-phase artifact derived from this contract, not authored here.
- Rate-limiting thresholds (mechanism established at the reverse-proxy layer, HLD §10; specific per-endpoint limits are an operational tuning decision, not an API contract decision).
- Full enumeration of every filter/sort field per list endpoint — representative sets given above; exhaustive field lists finalized during implementation against the actual query needs UI Design surfaces.

---

## 13. Approval & Next Steps

This API Design is **not final** until:
1. §11's project-existence-leakage judgment call (`403` vs. `404` for non-members) is confirmed or corrected.
2. This document is explicitly reviewed and approved.

Upon approval, the next phase is **UI Design**: screen inventory, component structure, and interaction design for the Admin Console, Backlog, Board, Sprint, and Reports views (HLD §9), consuming exactly the endpoints and payload shapes defined here. No implementation code will be written before UI Design is reviewed and approved.

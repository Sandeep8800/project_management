# Nexus PMS — UI Design

**Status:** Draft for review
**Phase:** 6 of 7 — Documentation-first sequence (PRD → HLD → LLD → Database Design → API Design → **UI Design** → Coding)
**Input:** [`01-PRD.md`](./01-PRD.md) · [`02-HLD.md`](./02-HLD.md) · [`05-api-design.md`](./05-api-design.md) (all Approved)
**Owners:** Architecture / Frontend
**Last updated:** 2026-07-13

> This document defines screen inventory, component structure, navigation, and interaction/state rules for the React + TypeScript SPA (HLD §9). It is not a visual/brand design spec — pixel-level layout, color, and typography are a design-tool (Figma) deliverable produced alongside or after this document, not authored here. Every screen below consumes exactly the endpoints defined in API Design; no new backend behavior is introduced at this phase.

---

## 1. Purpose & Scope

Translate the six backend modules and their API contracts into a concrete frontend structure: what screens exist, what data each fetches, how permissions gate interaction, and how real-time updates and optimistic-lock conflicts surface to the user. This is the last documentation phase — the next step is Coding.

---

## 2. Design Principles

1. **Server is the source of truth for authorization; UI reflects it, never enforces independently of it** (HLD §7, §9 — restated here because it directly shapes §7 below: every gated control fetches the resolved permission set and hides/disables accordingly, but a determined client bypassing the UI still gets `403` from the API).
2. **No self-service, ever, anywhere in the UI.** There is no signup screen, no "forgot password → set new password without admin involvement" flow, and no "create your own workspace" entry point. Every admin-governed constraint from PRD §1.1 has a corresponding *absence* in the UI, not just a hidden feature.
3. **One backlog, two board views** (HLD §4). The Backlog screen and both board screens all read/write the same issue set — switching between them is a view change, not a context switch, and should feel that way (shared filters, shared issue-detail panel component).
4. **Real-time by default, not opt-in.** Board and notification views subscribe to live updates (HLD §8.4) as soon as they mount; there is no manual "refresh" affordance required for collaborative awareness, though a manual refresh still exists as a fallback.
5. **Accessibility and browser support are requirements, not polish**, per HLD §13: WCAG 2.1 AA target, latest two versions of Chrome/Edge/Firefox/Safari.

---

## 3. Information Architecture & Navigation

```
Top-level nav (persistent shell)
 ├── Project Switcher (only projects the user has an active membership in)
 ├── [Selected Project] ──┬── Backlog
 │                        ├── Board (Scrum and/or Kanban tab, per project methodology)
 │                        ├── Sprints (planning/active/completed list)
 │                        └── Reports (Burndown / Velocity / CFD / Sprint Summary)
 ├── Notifications (bell icon, global — not project-scoped)
 ├── Admin Console (only rendered in nav if caller is platform Admin, HLD §1)
 └── Account menu (profile, notification preferences, logout)
```

- The **Project Switcher** is populated from the caller's own `project_memberships` (API §5 admin endpoint is admin-only; a non-admin user's own membership list is a lighter self-scoped endpoint, `GET /me/projects`, added here as a UI-driven addition to the API surface — flagged for confirmation since it wasn't explicit in API Design and should be added there before Coding, not invented ad hoc in frontend code).
- **Admin Console** is a fully separate section, not a set of buttons scattered across project screens — consistent with PRD's framing of governance as a distinct concern from day-to-day project work.
- A project's **Board tab** shows one or two sub-tabs depending on `methodology` (`SCRUM` → Scrum board only, `KANBAN` → Kanban board only, `HYBRID` → both, HLD §4) — not a settings toggle the user manages, since methodology is set by Admin at project creation (PRD FR-6) and isn't a UI-level preference.

---

## 4. Screen Inventory

### 4.1 Auth
| Screen | Key elements | Notes |
|---|---|---|
| **Login** | Email/password fields (local auth) + "Sign in with [IdP name]" button(s) per configured SSO provider | No "Create account" link anywhere on this screen — its absence is deliberate, not an oversight (Principle 2). |
| **SSO callback (transient)** | Loading state only | On `403 SSO_NO_MATCHING_ACCOUNT` (API §4), shows "No account found for this email — contact your administrator," never a "click here to sign up" recovery path. |

### 4.2 Admin Console
| Screen | Key elements | Backing endpoints |
|---|---|---|
| **User Management** | Table (name, email, department, status, default role), create-user modal, deactivate/reactivate actions, reset-password action | `GET/POST /admin/users`, `PATCH`, `/deactivate`, `/reactivate`, `/reset-password` |
| **Project Management** | Table (key, name, methodology, status, start date), create-project modal, archive/delete actions (delete disabled with a tooltip explaining "archive first" unless already archived — mirrors the API's `409 GOVERNANCE_SAFEGUARD`, PRD FR-8) | `GET/POST /admin/projects`, `/archive`, `DELETE` |
| **Project Memberships** (per project, reached from Project Management row) | Table of assigned users + role, add-user-to-project control (search existing users only — never an inline "create new user" shortcut, keeping user creation and project assignment as distinct governed actions), role change dropdown, remove action | `GET/POST/PATCH/DELETE /admin/projects/{id}/memberships` |
| **Admin Dashboard** | Summary tiles (project count, user count, active-vs-archived breakdown) + consolidated user↔project↔role table (PRD FR-16) | `GET /admin/dashboard` |
| **Audit Log** | Filterable table (actor, action type, target, date range), read-only, no export/edit affordance beyond viewing (PRD FR-17 requires queryable, not exportable, in v1) | `GET /admin/audit-log` |

### 4.3 Backlog
| Element | Behavior |
|---|---|
| Hierarchical list | Epics expand to show child Stories/Bugs; Stories expand to show child Sub-tasks. Flat filtered view available (e.g., "all Bugs") that breaks hierarchy grouping when a type filter is active. |
| Filter/search bar | `status, assignee, sprint, label, type, q` — maps directly to `GET /projects/{id}/issues` query params (API §6). |
| Drag-to-reorder | Only enabled for users with `EDIT_ISSUE` (Principle 1); calls the `backlog/reorder` endpoint; optimistic reorder in the list with rollback if the request fails. |
| Create Issue | Slide-over panel, not a full page navigation — type selector (Epic/Story/Task/Sub-task/Bug) drives which fields show (e.g., Sub-task requires a parent picker). |
| Issue row → Issue Detail panel | Opens the shared Issue Detail component (§4.6) as a side panel over the backlog, not a route change — preserves list scroll position and filters. |

### 4.4 Board (Scrum & Kanban)
| Element | Behavior |
|---|---|
| Columns | Derived from `board_columns` (API §7 `GET /projects/{id}/boards/{type}`); Scrum board columns show only the active sprint's issues, Kanban shows the full backlog grouped by status regardless of sprint (HLD §4). |
| Card drag between columns | Triggers `POST /issues/{id}/transitions`. If the target column maps to a status not legally reachable from the card's current status (workflow validation, API §6), the drop is rejected client-side using the already-fetched workflow graph (so illegal drops are visually blocked, not merely rejected after a round-trip) — but the server-side `422` check remains the actual authority if the client's cached workflow graph is stale. |
| WIP limit indicator (Kanban only) | Column header shows `count / limit`, turns to a warning state at the limit; a drop that would exceed it is rejected with the same "blocked at drop, confirmed by server" pattern as workflow validation. |
| Real-time updates | Board subscribes to `/topic/projects/{id}/boards/{type}` (API §10) on mount; a card move by another user animates into place without a manual refresh. |
| Sprint panel (Scrum board only) | Shows active sprint name/goal/dates, a live mini-burndown, and (for `MANAGE_SPRINT` holders) a "Complete Sprint" action opening the rollover-decision modal (§4.5). |
| Board config | `CONFIGURE_BOARD` holders only: edit columns, reorder, set WIP limits, edit workflow statuses/transitions (a dedicated sub-screen, not inline on the board, since workflow editing is infrequent and higher-risk than daily board use). |

### 4.5 Sprints
| Screen/Element | Behavior |
|---|---|
| Sprint list | Grouped by status (Planned / Active / Completed). |
| Create Sprint | `MANAGE_SPRINT` only. Name + goal; no dates required yet (LLD §6.1 — dates are set at Start). |
| Sprint Planning (on a `PLANNED` sprint) | Drag issues from backlog into the sprint (and back out); this is a view over the same Backlog data with a "planned for this sprint" filter, not a separate planning-specific issue list. |
| Start Sprint | Prompts for start/end dates; if another sprint is already active, the button is disabled with an explanatory tooltip rather than allowed-then-rejected — but the server's `409 SPRINT_ALREADY_ACTIVE` remains the authority (client-side disable is a convenience, consistent with Principle 1, since a second tab or stale client state could still race). |
| Complete Sprint | Modal requiring an explicit rollover choice (`MOVE_TO_BACKLOG` or pick a target sprint) before the "Complete" action is enabled — the modal cannot be dismissed into a default choice, directly reflecting PRD FR-25's "no silent default." |

### 4.6 Issue Detail (shared component, used from Backlog, Board, and direct links)
| Section | Behavior |
|---|---|
| Header | Key, type badge, title (inline-editable if `EDIT_ISSUE`). |
| Status control | Dropdown/button group showing **only the transitions legally reachable from the current status** per the project workflow (API §6, §7) — never a full status list with server-side rejection as the only guard. |
| Fields | Assignee, reporter, priority, story points, labels, sprint — each independently editable if `EDIT_ISSUE`, each a `PATCH` with `If-Match` (API §2.4); a `409` surfaces a non-destructive "this issue changed — reload to see the latest version" prompt rather than silently discarding the user's in-progress edit. |
| Linked issues | List with type (Blocks/Is Blocked By/Relates To/Duplicates), add/remove. |
| Comments | Chronological, `@mention` autocomplete against project members, posts to the shared comment feed regardless of role (any member can comment, API §6 note). |
| Attachments | Drag-and-drop upload using the two-step pre-signed-URL flow (API §6) — progress indicator during the direct-to-storage upload, then a second call registering metadata. |
| Activity/history | Read-only feed of status transitions and field changes — sourced from the same domain events the backend already publishes (LLD §9), not a separate UI-authored history mechanism. |

### 4.7 Reports
| Screen | Chart | Notes |
|---|---|---|
| Burndown | Line chart, remaining points/issues vs. days elapsed, ideal-line overlay | Per sprint (PRD FR-34). |
| Velocity | Bar chart, committed vs. completed points per sprint | Filter: last N sprints (PRD FR-35). |
| Cumulative Flow Diagram | Stacked area chart, issue count per status over time | Date range filter (PRD FR-36). |
| Sprint Summary | Table/summary card: planned vs. completed, scope added/removed, carry-over count | Per completed sprint (PRD FR-37). |

All four are pure read views — no edit affordances anywhere on these screens, reinforcing the Viewer/Stakeholder read-only guarantee (PRD FR-38) at the UI layer as well as the API layer (API §8).

### 4.8 Notifications
| Screen | Behavior |
|---|---|
| Bell dropdown (global nav) | Latest unread, real-time push via `/user/queue/notifications` (API §10) with a toast for new arrivals while the app is open. |
| Full notifications page | Paginated history, mark-read/mark-all-read. |
| Preferences | Single toggle: email delivery on/off (in-app is always on, HLD §8.1). |

---

## 5. Component System (Shared)

Reusable components spanning multiple screens, so module screens compose rather than reimplement:

| Component | Used by |
|---|---|
| `DataTable` (sortable, filterable, paginated — offset or cursor mode) | Admin lists, Backlog, Audit Log |
| `PermissionGate` (renders children only if the resolved permission set includes the given action; Principle 1) | Every mutating control across all screens |
| `BoardColumn` / `IssueCard` | Both board types |
| `IssueDetailPanel` | Backlog, Board, direct-link routes |
| `StatusBadge`, `PriorityBadge`, `TypeIcon` | Backlog, Board, Issue Detail, Reports |
| `RealtimeIndicator` (small connection-state dot: live / reconnecting / polling-fallback) | Board, Notifications |
| `ConflictToast` (surfaces `409 CONCURRENT_MODIFICATION` with a reload action) | Issue Detail, Sprint actions |
| `Modal`, `SlideOverPanel`, `Toast` | Global |

---

## 6. State Management & Data Fetching

- **Server state**: React Query (HLD §9), keyed by resource path + filters (e.g., `['issues', projectId, filters]`). Mutations invalidate the relevant query keys on success; board/backlog mutations additionally reconcile against real-time events so a self-triggered change doesn't double-apply when its own WebSocket echo arrives.
- **Optimistic updates**: drag-and-drop reorder, board column moves, and status transitions apply optimistically in the UI immediately, then reconcile with the server response — a failure (permission denied, workflow-invalid, conflict) rolls back the optimistic change and surfaces the specific error from §3's error model (API §3), not a generic failure toast.
- **Permission set**: fetched once per project context (piggybacked on project entry, not a separate round-trip per screen) and cached client-side for the session; invalidated and re-fetched if a `403` is ever received (covers the case where an admin changed the caller's role mid-session).
- **WebSocket lifecycle**: one connection per session (HLD §8.4), board/notification components subscribe/unsubscribe to specific destinations on mount/unmount rather than opening a new socket per screen.

---

## 7. Permission-Aware UI Pattern

`PermissionGate` is the single mechanism for role-based UI variation — no screen independently re-implements "if role is X, show Y." It consumes the cached permission set from §6 and either renders, disables (with a tooltip explaining why), or hides its children, per a per-usage prop — e.g., "Delete Issue" is hidden entirely for a Viewer but shown-disabled for a Developer who lacks `DELETE_ISSUE` specifically, since the latter case benefits from the user understanding *why* the action is unavailable rather than not knowing it exists at all. This is a UX convenience choice, not a security boundary — restated once more because it's the single most important invariant in this document (Principle 1).

---

## 8. Error, Empty, and Conflict States

- **404 for both "doesn't exist" and "you can't see it"** (per the API Design §11 decision to avoid leaking project existence): the UI renders one generic "Not found" screen for both cases — it does not attempt to distinguish them, since the API deliberately doesn't either.
- **409 Concurrent Modification**: non-destructive `ConflictToast` (§5) — never a silent overwrite or a forced page reload that discards unsaved input elsewhere on the screen.
- **422 Invalid Workflow Transition**: surfaced inline at the point of the attempted transition (board drop or status dropdown), not a generic error banner, since the user needs to see *which* transition failed.
- **Empty states**: an empty backlog, empty board column, or a project with no completed sprints (blocking Reports) each get a specific empty-state message with the relevant next action (e.g., "No completed sprints yet — reports will appear after your first sprint completes"), not a blank screen.

---

## 9. Accessibility & Browser Support

Carried forward from HLD §13 as binding requirements for this phase, not aspirational:
- WCAG 2.1 AA: keyboard navigability for all interactive elements including drag-and-drop (board/backlog reordering needs a keyboard-operable alternative, not drag-only), sufficient color contrast for all status/priority badges, screen-reader labels on icon-only controls.
- Browser support: latest two versions of Chrome, Edge, Firefox, Safari.
- Real-time features degrade gracefully (SSE fallback, API §10) rather than being a hard requirement — a WebSocket-blocked environment still gets a functional, if less immediate, board.

---

## 10. Out of Scope for This Phase

- Pixel-level visual design (color palette, typography, spacing system, component library choice such as shadcn/ui vs. MUI) — a design-tool/implementation-phase deliverable, not authored in this document.
- Detailed wireframes/mockups per screen — the screen inventory (§4) defines structure and behavior; visual layout is produced separately.
- Mobile-native app design (PRD §6 — explicitly out of scope for the product, not just this phase).
- The `GET /me/projects` endpoint gap noted in §3 — needs to be added to API Design before Coding, not designed here.

---

## 11. Approval & Next Steps

This UI Design is **not final** until:
1. The `GET /me/projects` endpoint gap (§3) is confirmed and folded back into API Design.
2. This document is explicitly reviewed and approved.

Upon approval, the next and final phase is **Coding** — implementation of the Spring Boot backend (per HLD/LLD/Database Design/API Design) and the React + TypeScript frontend (per this document), against the fully-approved documentation chain. No code has been written prior to this point in the process.

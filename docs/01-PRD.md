# Nexus PMS — Product Requirements Document (PRD)

**Status:** Draft for review
**Phase:** 1 of 7 — Documentation-first sequence (PRD → HLD → LLD → Database Design → API Design → UI Design → Coding)
**Owners:** Product / Architecture
**Last updated:** 2026-07-13

> This document must be reviewed and its open questions resolved before High-Level Design (HLD) begins. No implementation code is written against this phase.

---

## 1. Problem Statement & Goals

### 1.1 Problem Statement

Software organizations running Scrum, Kanban, or hybrid delivery need a system of record for planning, tracking, and reporting on work — epics, stories, tasks, sub-tasks, and bugs — across sprints and boards. Commercial tools (Jira, Azure DevOps, ClickUp) solve this but come with licensing cost, vendor lock-in, and governance models that don't always match an organization's internal access-control policies.

Nexus PMS is a self-hosted, single-tenant, **admin-governed** alternative purpose-built for this class of problem. "Admin-governed" is the defining constraint: unlike SaaS project tools that allow self-service signup and ad-hoc workspace creation, nothing in Nexus PMS — no user, no project, no role — exists unless an administrator explicitly provisions it. This makes the platform suitable for organizations with strict IT governance, compliance, or HR-linked identity requirements, where uncontrolled account or project sprawl is unacceptable.

### 1.2 Goals

- Provide full Agile/Scrum and Kanban work-tracking capability (backlog, sprints, boards, reporting) comparable to industry-standard tools.
- Enforce centralized, admin-driven control over who exists in the system, which projects exist, and who can do what on each project.
- Support project-scoped RBAC, where a user's role and permissions can differ per project.
- Integrate with enterprise identity providers (SSO) so authentication follows existing organizational identity, while account existence remains admin-controlled.
- Provide a durable audit trail of all administrative and governance actions.
- Serve as the system of record for sprint execution and reporting (burndown, velocity, cumulative flow).

### 1.3 Non-Goals (for this phase)

- Public/self-service signup of any kind.
- Multi-tenant SaaS operation (see §4, resolved: single-tenant).
- Full feature parity with every Jira/Azure DevOps capability (e.g., marketplace plugin ecosystems, advanced automation rule engines) — see §6, Out of Scope.

---

## 2. User Personas

| Persona | Summary | Primary Goals | Key Pain Points Today |
|---|---|---|---|
| **Admin** | IT/platform owner responsible for provisioning users, projects, and access. | Onboard/offboard staff quickly and correctly; keep an auditable record of who has access to what; prevent unauthorized project or account creation. | Manual, spreadsheet-driven access tracking; no single view of user↔project↔role mappings; no audit trail. |
| **Project Manager / Scrum Master** | Runs day-to-day delivery for one or more projects. | Configure and run sprints, manage the backlog, keep the board and team aligned, remove blockers. | Needs project-scoped control without needing platform-admin rights; needs visibility into team capacity and sprint health. |
| **Product Owner** | Owns the "what" and "why" — backlog priority and acceptance criteria. | Prioritize backlog, define acceptance criteria, accept/reject completed work. | Needs fast backlog reordering and clear traceability from epic to story to task. |
| **Developer** | Implements the work. | See what's assigned, update status quickly, log time/comments, understand linked issues and blockers. | Friction in status updates and context-switching between boards. |
| **QA / Tester** | Owns bug lifecycle and verification. | Log and triage bugs as first-class issues, track test cases, verify fixes before closure. | Bugs often live in a separate tool disconnected from the sprint/backlog. |
| **Viewer / Stakeholder** | Executive or cross-functional observer. | Check status/reports without needing to understand tool mechanics or risk editing anything. | Either over-privileged (can accidentally edit) or locked out entirely today. |

Roles are **project-scoped** (§3.1.3): the same person may hold different roles on different projects (e.g., Scrum Master on Project A, Developer on Project B). Admin is the one role with platform-wide (cross-project) scope in addition to any project-level roles.

---

## 3. Functional Requirements

### 3.1 Admin Capabilities

#### 3.1.1 User Management
- FR-1: Admin can create user accounts with: name, email, employee ID, department, default role. No public/self-service registration path exists anywhere in the system.
- FR-2: Admin can deactivate a user account (revokes access, preserves historical attribution on issues/comments/audit records).
- FR-3: Admin can reactivate a previously deactivated account.
- FR-4: Admin can trigger password resets for local-credential accounts, or trigger SSO-linked account provisioning for federated accounts.
- FR-5: Deactivated users are removed from active assignment pools (cannot be newly assigned issues) but remain visible in historical records (audit log, past comments, resolved issues) for traceability.

#### 3.1.2 Project Management
- FR-6: Admin can create a project with: project key/code (unique, short identifier used in issue IDs, e.g. `NEX`), name, description, methodology type (Scrum / Kanban / Hybrid), start date, target release.
- FR-7: Admin can archive a project (read-only, hidden from active project lists, data retained).
- FR-8: Admin can delete a project only behind a safeguard (e.g., archive-first requirement, confirmation step, and/or soft-delete with retention window) to prevent irreversible loss of history.
- FR-9: Project key is immutable once issues have been created under it (issue IDs are derived from the key and must remain stable).

#### 3.1.3 User-to-Project Assignment & Role Mapping
- FR-10: Admin can assign one or more users to a project.
- FR-11: Admin assigns exactly one role per user per project (project-scoped role, not global) from the core role set: Admin, Project Manager/Scrum Master, Product Owner, Developer, QA/Tester, Viewer/Stakeholder.
- FR-12: A single user may simultaneously hold different roles on different projects.
- FR-13: The system maintains a permissions matrix mapping each role to allowed actions (create issue, edit issue, transition status, delete issue, manage sprint, configure board, view reports, manage users on project, etc.). This matrix is enforced server-side (see §4, RBAC).
- FR-14: Admin can view and edit a user's full set of project-role assignments from a single screen (per-user view), and a project's full set of assigned users/roles from a single screen (per-project view).

#### 3.1.4 Audit & Governance
- FR-15: All admin actions are logged: user creation/deactivation/reactivation, project creation/archival/deletion, role assignment changes — each with timestamp and acting admin identity.
- FR-16: Admin dashboard provides a consolidated view of all projects, all users, and their current role assignments.
- FR-17: Audit log is append-only and queryable/filterable (by actor, action type, date range, target entity) for compliance review.

### 3.2 Backlog Management
- FR-18: Support issue type hierarchy: Epic → Story → Task → Sub-task, plus Bug as a first-class, independently trackable issue type (may optionally link to a Story/Epic).
- FR-19: Backlog view supports create, reorder (drag/prioritize), and filter of issues within a project.
- FR-20: Issues support parent/child linkage consistent with the hierarchy above (e.g., Story belongs to an Epic; Sub-task belongs to a Task or Story).

### 3.3 Sprint Lifecycle (Scrum-enabled projects)
- FR-21: Create a sprint (name, start date, end date, goal).
- FR-22: Sprint planning: add/remove backlog issues to/from a sprint before it starts (drag-and-drop or equivalent).
- FR-23: Start a sprint (locks sprint dates, transitions sprint to Active; only one active sprint per board unless the org explicitly enables parallel sprints — flagged as an open design question for HLD).
- FR-24: Active Sprint board reflects real-time status of all issues in the sprint.
- FR-25: Complete a sprint: incomplete issues are handled via explicit rollover decision at completion time — return to backlog or move to the next sprint (PM/Scrum Master choice at completion, not silent default).

### 3.4 Boards
- FR-26: Kanban board: continuous-flow board, not sprint-scoped, with configurable per-column WIP limits.
- FR-27: Scrum board: sprint-scoped board reflecting the active sprint's issues.
- FR-28: Board availability is determined by project methodology type (Scrum / Kanban / Hybrid), set at project creation (FR-6). **Whether Hybrid means "both board types view the same backlog" vs. "mutually exclusive per-issue tracks" is an open question — see §5.**
- FR-29: Board columns map to a status workflow that is configurable per project (see FR-30).

### 3.5 Issue Details
- FR-30: Default status workflow: To Do → In Progress → In Review → Done, customizable per project (add/rename/reorder statuses, define allowed transitions).
- FR-31: Issue fields: assignee, reporter, priority, story points/estimate, labels, comments, attachments, linked issues (blocks/is blocked by/relates to/duplicates).
- FR-32: Status transitions, assignment, and edits are subject to project-scoped RBAC (a Viewer cannot transition status; a Developer cannot delete an issue unless granted; etc. — matrix per FR-13).
- FR-33: Comments support @mentions, which trigger notifications (§3.7).

### 3.6 Reporting
- FR-34: Burndown chart (sprint scope): remaining work vs. time, per active/completed sprint.
- FR-35: Velocity chart: completed story points per sprint, trended across sprints, per project/team.
- FR-36: Cumulative Flow Diagram (CFD): issue counts per status over time, for Kanban and Scrum boards.
- FR-37: Sprint summary report: planned vs. completed, scope changes during sprint, carry-over.
- FR-38: Reports are read-accessible to any project role including Viewer/Stakeholder (read-only by definition); no edit capability implied.

### 3.7 Notifications
- FR-39: Notify on: issue assignment change, @mention in a comment, status transition on an issue the user is watching/assigned to, sprint start, sprint end.
- FR-40: Notification delivery channel(s) (in-app only vs. email vs. both) — open implementation detail for HLD, not blocking PRD approval.

---

## 4. Non-Functional Requirements

| Area | Requirement | Status |
|---|---|---|
| **Tenancy** | Single-tenant architecture. One deployment serves one organization. No `tenant_id` partitioning or cross-tenant isolation logic required in schema or query layer. | **Resolved** |
| **Authentication** | Auth module supports SAML, OAuth2, and OIDC for enterprise IdPs (Okta, Azure AD, Google Workspace), alongside or instead of local credential login. SSO governs *authentication* only — account *existence* is always admin-provisioned (no just-in-time account creation via SSO first login, unless explicitly reconsidered at HLD). | **Resolved** |
| **Authorization (RBAC)** | Enforced at the API layer. Every API endpoint validates the caller's project-scoped role server-side before executing an action. UI-level hiding of controls is a convenience only, never the authorization boundary. | **Resolved** |
| **Auditability** | All admin governance actions (§3.1.4) are logged durably and are queryable. | Resolved (requirement level); retention period TBD at HLD. |
| **Scale** | Concurrent users, number of projects, issues per project. Needed to size DB indexing, caching, and connection pooling strategy. | **Open — blocks HLD sizing decisions, see §5.** |
| **Availability/DR** | Not yet specified (target uptime, backup/restore RPO/RTO). | Open — to be scoped at HLD given this is single-tenant self-hosted. |
| **Data retention** | Archived/deleted project retention window, audit log retention period. | Open — to be scoped at HLD. |

---

## 5. Open Questions Requiring Stakeholder Decision Before HLD

These must be resolved (or explicitly deferred with a documented default) before HLD design begins:

1. **Expected scale** — concurrent users, number of projects, and issues per project. Required to size database indexing strategy, caching layer, and connection pooling. *No default assumed; blocking.*
2. **Kanban/Scrum coexistence** — for a project marked "Hybrid" (or generally), do Kanban and Scrum boards operate as two views over the *same* backlog and issue set, or are they mutually exclusive project-type tracks that don't share a backlog? This determines board/data-model design (FR-28) and sprint semantics (FR-23).
3. Related sub-question: can a project run **multiple concurrent active sprints** on one board, or is it strictly one active sprint per project/board at a time (FR-23 default assumption)?
4. Notification delivery channels — in-app, email, or both (FR-40) — non-blocking for PRD sign-off but should be confirmed early in HLD.
5. SSO account linkage — does first SSO login for a pre-provisioned (by-email) admin-created account auto-link, or does an admin need to explicitly bind the SSO identity to the local account record? (Governs the "no self-service" guarantee at the account-linkage boundary.)

Resolved and no longer open:
- ~~Multi-tenant vs single-tenant~~ — **Resolved: single-tenant.**
- ~~SSO priority~~ — **Resolved: SAML/OAuth2/OIDC supported.**
- ~~RBAC enforcement layer~~ — **Resolved: enforced at the API layer.**

---

## 6. Out of Scope (This Phase)

- Public/self-service account registration (explicitly and permanently excluded from the product, not just this phase).
- Multi-tenant SaaS deployment model.
- Marketplace/plugin ecosystem or third-party app integrations beyond SSO identity providers.
- Advanced workflow automation rule engines (e.g., "when X then Y" automation builders).
- Native mobile applications (web-responsive only, unless revisited post-MVP).
- Time-tracking/billing/invoicing beyond basic time-logging on issues (FR-31 covers logging, not billing).
- Portfolio/program-level roll-up reporting across projects (single-project reporting only for MVP; cross-project reporting is a candidate for a later phase).
- Advanced test-management suite (QA role covers bug lifecycle and basic test cases per FR set; a dedicated test-case management module with test plans/runs is out of scope for MVP).

---

## 7. Approval & Next Steps

This PRD is **not final** until:
1. Open questions in §5 are resolved by stakeholders (at minimum, expected scale and Kanban/Scrum coexistence, as these directly shape data model and board architecture).
2. This document is explicitly reviewed and approved.

Upon approval, the next phase is **High-Level Design (HLD)**: system architecture, service boundaries, technology choices within the Spring Boot / React + TypeScript stack, and how the resolved NFRs (single-tenant, SSO, API-layer RBAC) map onto concrete architecture components. No implementation code will be written before HLD and subsequent LLD, Database Design, API Design, and UI Design phases are each reviewed in turn.

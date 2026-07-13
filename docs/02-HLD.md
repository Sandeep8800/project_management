# Nexus PMS — High-Level Design (HLD)

**Status:** Approved
**Phase:** 2 of 7 — Documentation-first sequence (PRD → **HLD** → LLD → Database Design → API Design → UI Design → Coding)
**Input:** [`01-PRD.md`](./01-PRD.md) (Approved, 2026-07-13)
**Owners:** Architecture
**Last updated:** 2026-07-13

> This document defines system architecture, service boundaries, and technology choices. It does not define database schemas (Database Design phase), API contracts (API Design phase), UI layouts (UI Design phase), or class/module-internal design (LLD). No implementation code is written against this phase.

> **Revision note (2026-07-13):** this draft incorporates an external architecture review. Gaps that were genuine under-specifications of already-approved PRD requirements — real-time board updates (PRD FR-24), background job processing, explicit workflow-engine treatment (PRD FR-30), deployment/HA hardening, DR detail, and measurable NFR targets — are addressed in §8, §10, §12, §13 below. Suggestions that would expand product scope beyond the approved PRD (automation engine, plugin/marketplace architecture, third-party integrations/webhooks, field/issue-level permission schemes, and enterprise modules such as time tracking, budgets, roadmaps, and wiki) were **not** added — stakeholder decision was to keep current PRD scope (§16).

---

## 1. Purpose & Scope

Translate the approved PRD requirements and resolved decisions into a concrete system architecture that:

- Supports the **Large** scale target: 300+ concurrent users, 100+ projects, 10k+ issues/project (~1M+ issues system-wide at ceiling).
- Enforces **API-layer RBAC** as the sole authorization boundary, with project-scoped roles.
- Supports **SSO (SAML/OAuth2/OIDC)** alongside local credentials, with auto-link-by-email account linkage.
- Operates as a **single-tenant** deployment (no tenant partitioning logic anywhere in the stack).
- Realizes the **shared-backlog, dual-view** board model (Scrum + Kanban over one backlog).
- Delivers on the **admin-governed** philosophy: every write path for users/projects/roles flows through admin-authorized operations, fully audited.

---

## 2. Architecture Style & Rationale

**Decision: Modular Monolith**, deployed as a single Spring Boot application (horizontally scaled, stateless instances), fronted by a React + TypeScript SPA, backed by PostgreSQL + Redis.

**Rationale:**
- The domain (backlog, sprints, boards, issues, RBAC, audit) is highly relational and transactionally coupled — an issue's status transition touches board state, sprint metrics, notifications, and audit in one logical unit of work. A modular monolith keeps these transactions ACID and simple; microservices would force distributed transactions or eventual-consistency workarounds for no real benefit at this scale.
- The Large-scale target (300+ concurrent users, ~1M issues ceiling) is well within the proven capacity of a well-indexed PostgreSQL primary + read replicas + Redis caching, fronting a horizontally-scaled stateless app tier. This does **not** require microservice decomposition to hit target scale.
- Single-tenant, self-hosted deployment means there's no per-tenant scaling isolation requirement that would otherwise push toward service decomposition.
- Module boundaries are enforced at the **package/component level** (bounded contexts, see §4) so that any module can be extracted into a standalone service later if a specific module's load profile genuinely diverges (e.g., Reporting becomes a separate read-optimized service under heavier future scale). This is an explicit escape hatch, not a near-term plan.

**Rejected alternative:** Microservices-from-day-one. Rejected due to added operational complexity (service mesh, distributed tracing, saga orchestration for cross-module transactions) that isn't justified by the confirmed scale target, and would slow delivery of the core admin-governance and Agile feature set.

---

## 3. System Context

```
                        ┌─────────────────────────┐
                        │   Enterprise IdP         │
                        │ (Okta / Azure AD /       │
                        │  Google Workspace)       │
                        │  SAML / OAuth2 / OIDC    │
                        └───────────┬─────────────┘
                                    │ federated auth
                                    ▼
 ┌───────────┐   HTTPS    ┌──────────────────────┐
 │  Browser   │◄──────────►│   Nexus PMS Web App   │
 │ (React SPA)│            │ (React + TypeScript)  │
 └───────────┘            └───────────┬───────────┘
                                       │ REST/JSON over HTTPS (JWT bearer)
                                       ▼
                        ┌───────────────────────────┐
                        │  Nexus PMS API             │
                        │  (Spring Boot, modular     │
                        │   monolith, stateless,     │
                        │   horizontally scaled)     │
                        └───────┬──────────┬─────────┘
                                │          │
                    ┌───────────┘          └───────────┐
                    ▼                                  ▼
          ┌──────────────────┐               ┌──────────────────┐
          │  PostgreSQL        │               │      Redis          │
          │  (primary + read   │               │ (cache, sessions,   │
          │   replicas)        │               │  rate limiting)     │
          └──────────────────┘               └──────────────────┘
                    │
                    ▼
          ┌──────────────────┐
          │  Object Storage    │
          │  (S3-compatible)   │
          │  issue attachments │
          └──────────────────┘

          Async, outbox-driven ▶  Notification Dispatch (in-app + email)
```

**External actors:** Admin, Project Manager/Scrum Master, Product Owner, Developer, QA/Tester, Viewer/Stakeholder (all via browser SPA) — no external API consumers in scope for v1 (a public REST API for third-party integration is a candidate future enhancement, not v1).

**External systems:** Enterprise IdP (SSO), SMTP/email provider (notification delivery), object storage (attachments).

---

## 4. Module Boundaries (Bounded Contexts)

Each module owns its data and exposes behavior only through its own service interfaces; no module reaches into another module's tables directly. Cross-module coordination happens through in-process application events (Spring `ApplicationEventPublisher`) plus a transactional outbox for anything that must survive a crash before delivery (notifications, audit).

| Module | Owns | Depends on |
|---|---|---|
| **Identity & Access** | User accounts, local credentials, SSO identity links, sessions/tokens | — |
| **Governance & RBAC** | Projects, project-role assignments, role→permission matrix, audit log | Identity & Access |
| **Backlog & Issues** | Epics/Stories/Tasks/Sub-tasks/Bugs, issue links, comments, attachments, labels | Governance & RBAC (authorization), Identity & Access (assignee/reporter) |
| **Sprint & Board** | Sprints, board configuration (Scrum/Kanban), WIP limits, **workflow engine** (status definitions, allowed transitions, per-project customization) | Backlog & Issues, Governance & RBAC |
| **Reporting** | Burndown, velocity, CFD, sprint summary (read-optimized projections) | Sprint & Board, Backlog & Issues (read-only, via projections not live joins) |
| **Notifications** | In-app + real-time notifications, email dispatch, delivery preferences | Identity & Access, event feed from all modules |

This mirrors PRD §3's grouping (Admin, Backlog, Sprint, Board, Reporting, Notifications) directly, so requirements traceability from PRD → module is 1:1.

**Workflow as an explicit concern, not a side detail.** PRD FR-30 requires per-project customizable status workflows (add/rename/reorder statuses, define allowed transitions). Architecturally this is a **workflow engine sub-component within Sprint & Board**: a per-project status-graph definition plus a transition-validation service that every status-change request passes through (checked *after* RBAC authorization, since "is this transition allowed by the workflow" and "is this user allowed to make transitions" are separate concerns). It is not exposed as a standalone top-level module in v1 because nothing else in the current PRD scope needs to invoke it independently of Sprint & Board — but it is designed as an isolated sub-component so it *could* be extracted or exposed separately later (e.g., if a future Automation module needs to reference workflow definitions).

**Future extraction points (not built in v1, scope kept per PRD §6).** The module boundaries above are deliberately drawn so that these commonly-requested enterprise capabilities — raised in external review but out of the approved PRD scope — could be added as new modules later without restructuring the existing six: a **Search** module (behind the same query interface Backlog & Issues already exposes, see §5), an **Automation** module (subscribing to the same domain events Reporting and Notifications already consume, see §8.6), an **Integration/Webhook** module (an additional outbound consumer of the domain event bus), and a **Plugin** extension layer (would sit alongside the module boundary, not inside it). None of these are designed in detail here — see §16.

---

## 5. Data Architecture

- **Primary datastore: PostgreSQL.** Relational model fits the strongly-typed hierarchy (Epic→Story→Task→Sub-task, Bug) and referential-integrity needs (project-role assignments, issue links, audit trail) better than a document store. Full ACID transactions matter for admin governance actions (e.g., role reassignment) and sprint transitions.
- **Read replicas.** Reporting queries (burndown, velocity, CFD) and read-heavy list views (backlog, board) read from replica(s); all writes go to the primary. This isolates report/query load from transactional write load — critical at the Large-scale target where 100+ projects generate concurrent reporting traffic.
- **Redis cache**, used for three distinct purposes (separate key namespaces / logical DBs):
  1. Hot-read cache for active sprint board and Kanban board state (short TTL + explicit invalidation on write).
  2. Resolved permission cache: (user, project) → effective role + permission set, invalidated on role-assignment change, avoiding a DB round-trip on every authorized request.
  3. Session/token cache for JWT revocation lists and SSO session state.
- **Object storage (S3-compatible)** for issue attachments — never stored as DB blobs. DB stores only metadata (filename, size, content-type, storage key).
- **Reporting is projection-based, not live-aggregation.** Burndown/velocity/CFD are computed incrementally as issue/sprint/status-change events occur (event-driven materialized projections in dedicated reporting tables), not recalculated from raw issue history on every page load. This is the key scalability lever for reporting at the Large-scale target and avoids expensive on-demand aggregation queries across 10k+ issues per project.
- **Search:** PostgreSQL full-text search (`tsvector`) for issue search/filtering in v1, accessed behind a dedicated `IssueSearchService` interface within Backlog & Issues rather than ad-hoc queries scattered across controllers. This interface boundary is the deliberate seam for swapping the implementation to a dedicated search engine (e.g., OpenSearch) without touching callers. **Trigger for revisit at Database Design phase:** sustained multi-facet filter queries (project + assignee + sprint + label + date range + free text, combined) against >200k issues, where Postgres FTS/B-tree combinations degrade — not built in v1 to avoid premature infrastructure ahead of that trigger.

---

## 6. API Layer

- **Style:** RESTful JSON API, versioned (`/api/v1/...`), resource-oriented per module (`/api/v1/projects`, `/api/v1/projects/{id}/backlog`, `/api/v1/projects/{id}/sprints`, `/api/v1/admin/users`, etc.). Full contract defined in the API Design phase.
- **Why REST over GraphQL:** the RBAC model requires a clear, auditable enforcement point per action (create issue, transition status, delete issue, etc. — PRD FR-13). REST's resource+verb model maps directly onto the permission matrix's action list. GraphQL's flexible query shape would require field-level authorization logic that's harder to audit and reason about against a fixed permission matrix — rejected for that reason.
- **RBAC enforcement point:** a single, shared `AuthorizationInterceptor`/`@PreAuthorize`-backed policy layer sits in front of every controller method in every module. It resolves `(authenticated user, target project, requested action)` → allow/deny using the cached permission resolution from §5. **No module implements its own ad-hoc authorization logic** — this guarantees the "server is the source of truth, UI is convenience only" NFR is structurally enforced, not just convention.
- **AuthN on every request:** short-lived JWT access token (issued after local-credential or SSO login) validated on every request; refresh token rotation for session continuity. No server-side sticky sessions — keeps the app tier stateless and horizontally scalable.

---

## 7. Security Architecture

| Concern | Approach |
|---|---|
| Authentication | Spring Security: local credential login (bcrypt-hashed passwords) OR federated SSO (SAML2 / OAuth2 / OIDC) against enterprise IdPs. Both issue the same internal JWT after successful auth — downstream modules never need to know which auth method was used. |
| Account linkage | First successful SSO login auto-links by exact email match to an existing admin-provisioned account (PRD FR-4). No account is ever created by the auth flow itself. |
| Authorization | API-layer RBAC per §6 — project-scoped role → permission matrix, enforced server-side on every endpoint. |
| Transport | TLS everywhere (browser↔app, app↔DB, app↔Redis, app↔IdP). |
| Secrets | Externalized configuration (environment variables / secrets manager, e.g. Vault or cloud-native secrets store); nothing sensitive in source or images. |
| Audit | Every admin governance action (user create/deactivate/reactivate, project create/archive/delete, role assignment change) is written to an append-only audit log synchronously within the same transaction as the action itself — audit entries are never best-effort or async, since compliance requires no possibility of a governance action succeeding without a matching audit record (PRD FR-15–17). |
| Password reset / SSO provisioning triggers | Admin-initiated only (PRD FR-4); no self-service password reset flow exists, consistent with the no-self-service platform philosophy. |

---

## 8. Cross-Cutting Concerns

### 8.1 Notifications
Event-driven: state changes (assignment, @mention, status transition, sprint start/end) publish an internal event; the Notifications module consumes it via a transactional outbox (write the "notification to send" row in the same DB transaction as the triggering change, then dispatch asynchronously). This guarantees no notification is silently lost if the dispatch step fails transiently, without coupling the triggering transaction to a slow email send. In-app notifications are always generated; email dispatch is a delivery channel toggle per user (PRD FR-40 — channel scope confirmed at HLD kickoff, per-user opt-in for email on top of always-on in-app).

### 8.2 Reporting Projections
As noted in §5, reporting data is maintained as incrementally-updated projections rather than computed live. A dedicated internal event stream (issue created/moved/status-changed, sprint started/completed) feeds projection updates. This keeps report load (burndown/velocity/CFD, PRD FR-34–37) cheap to serve even at 10k+ issues/project, and isolates reporting compute from the transactional write path.

### 8.3 Audit Log
Treated as a first-class module output, not a side effect of logging. Append-only table, indexed by actor/action-type/date/target-entity for compliance queries (PRD FR-17), written synchronously as described in §7.

### 8.4 Real-Time Updates
PRD FR-24 requires the Active Sprint board to "reflect real-time status" of sprint issues, and the same expectation applies to the Kanban board — when one user moves an issue, others viewing the same board should see it move without a manual refresh. This was under-specified in the initial draft and is addressed here:

- **Transport:** WebSocket (STOMP over WebSocket, via Spring's WebSocket support) for authenticated, per-board subscriptions. Server-Sent Events (SSE) is the fallback for environments where WebSocket is blocked by network policy — both push the same event payloads.
- **Scope of push:** board-state changes (issue moved/created/updated on a board the client is currently viewing) and in-app notification delivery (§8.1). Not a general-purpose pub/sub for all data — only board and notification views subscribe.
- **Authorization:** a board subscription is authorized exactly like a REST read of that board (§6, §7) — a client cannot subscribe to a project's board data it doesn't have read access to. The same permission cache from §5 backs this check.
- **Source of events:** the same domain events published for Reporting/Notifications (§8.6) — real-time push is one more consumer of that stream, not a separate change-detection mechanism.
- **Statelessness impact:** WebSocket connections are inherently stateful at the transport level, which is the one place this HLD's "fully stateless app tier" claim (§10) needs a caveat — addressed via a shared subscription registry in Redis (pub/sub) so any app instance can publish an event that reaches a client connected to a *different* instance, keeping horizontal scaling intact.

### 8.5 Background Job Processing
Several PRD-required and architecturally-necessary operations must not run inline on the HTTP request thread: sprint-completion rollover processing (PRD FR-25, potentially touching many issues at once), reporting projection rebuilds/backfills, scheduled reminder notifications, bulk backlog operations, and email dispatch retries.

- **Mechanism:** a durable job table (`scheduled_job` / `job_queue`) written to within the same transaction as the triggering action (outbox-style, consistent with §8.1/§8.6), polled by a worker pool running inside the same Spring Boot application (`@Scheduled` + a bounded executor), not a separate service in v1 — consistent with the modular-monolith decision in §2.
- **Job types (v1):** sprint-completion rollover, notification/email dispatch, reporting-projection updates, scheduled reminders (sprint start/end).
- **Idempotency:** every job is safe to re-run (jobs operate on durable state and check "already applied" before acting), so worker crashes or duplicate pickups under horizontal scaling don't corrupt state.
- **Extraction point:** if job volume or latency requirements outgrow an in-process worker pool, this table-based queue can be replaced by an external queue (e.g., SQS, RabbitMQ) behind the same job-submission interface — not needed at the Large-scale target, flagged as a future option only.

### 8.6 Domain Event Bus (Abstraction)
Formalizing what was implicit in earlier sections: all cross-module coordination (§8.1 Notifications, §8.2 Reporting, §8.4 Real-Time, §8.5 Background Jobs, and audit in §8.3) flows through one internal abstraction, a `DomainEventPublisher` interface, currently implemented via Spring's `ApplicationEventPublisher` plus the transactional outbox table for anything that must survive a process crash before delivery.

Every module publishes domain events (e.g., `IssueStatusChanged`, `SprintCompleted`, `ProjectRoleAssigned`) through this single interface rather than calling other modules' services directly or bypassing it with ad-hoc mechanisms. Consumers (Reporting projections, Notifications, real-time push, audit, and future Search/Automation/Integration modules per §4) all subscribe the same way. Because the publish/subscribe contract is fixed regardless of transport, the in-process implementation can be swapped for an external broker (Kafka/RabbitMQ) later — by changing the `DomainEventPublisher` implementation only — if a module is ever extracted into its own service (§2). No external broker is introduced in v1; see §14 for the explicit rationale.

---

## 9. Frontend Architecture

- **React + TypeScript SPA**, structured by the same module boundaries as the backend (Admin Console, Backlog, Board, Sprint, Reports, Auth) to keep frontend/backend mental models aligned.
- **Server state** (issues, boards, sprints, reports) managed via a query/cache library (e.g., React Query) — not global Redux state — since almost all app state is server-derived and needs cache invalidation semantics, not client-owned state.
- **UI-level permission hints** (hiding/disabling buttons a user's role can't use) are a pure UX convenience layer that mirrors the server's permission matrix for responsiveness — they are never the enforcement point (§6, §7). Every mutating action still round-trips through server-side authorization.
- Full component/screen inventory deferred to the UI Design phase.

---

## 10. Deployment Architecture

```
 Internet
    │
    ▼
 ┌───────────────────────┐
 │ Reverse Proxy / Gateway │  TLS termination, rate limiting,
 │ (Nginx / Traefik)        │  request logging, correlation-ID
 └───────────┬────────────┘  injection, static-asset + attachment
             │ HTTPS           serving (CDN-fronted where available)
             ▼
 ┌───────────────────────┐
 │   Load Balancer          │
 └───────┬───────┬─────────┘
         ▼       ▼
   ┌─────────┐ ┌─────────┐   ...N stateless app instances
   │  App 1    │ │  App 2    │   (horizontal autoscale)
   └────┬────┘ └────┬────┘
        └──────┬──────┘
               ▼
   ┌─────────────────────┐        ┌───────────────────┐
   │ PostgreSQL Primary    │◄──────►│ PostgreSQL Standby  │  (sync/near-sync
   │                        │        │ (failover target)   │   replication)
   └──────────┬────────────┘        └───────────────────┘
              │
              ▼
   ┌─────────────────────┐
   │ Read Replica(s)        │  (Reporting + read-heavy queries, §5)
   └─────────────────────┘

   ┌─────────────────────┐
   │  Redis (Sentinel or     │  cache + WebSocket pub/sub registry (§8.4)
   │  Cluster mode for HA)   │
   └─────────────────────┘
```

- **App tier:** containerized Spring Boot instances, stateless, horizontally scaled behind a load balancer. No session affinity required (JWT-based auth, §6) — the one caveat is WebSocket connections (§8.4), which are handled via the Redis-backed subscription registry rather than sticky sessions, so any instance can serve any client.
- **Reverse proxy / API gateway layer:** sits in front of the load balancer (or combined with it), terminating TLS and providing rate limiting, request logging, and correlation-ID injection ahead of the app tier — pulled out as its own layer per the external review's point that these cross-cutting HTTP concerns shouldn't be reimplemented inside application code. A lightweight reverse proxy (Nginx/Traefik) is sufficient at this scale; a full API-management product is not justified for a single-tenant deployment with no external API consumers (§3).
- **Static assets and attachments:** served from object storage (§5), fronted by a CDN where the deployment environment provides one (self-hosted deployments without a CDN fall back to serving directly from object storage/reverse-proxy caching — acceptable at single-tenant scale, upgraded opportunistically rather than required).
- **Data tier:** PostgreSQL primary + synchronous/near-synchronous standby (failover) + read replica(s) (§12); Redis in Sentinel or Cluster mode for cache/session/WebSocket-registry high availability, not a single instance; S3-compatible object storage for attachments.
- **Self-hosted, single-tenant:** one full stack deployment per organization, consistent with the resolved single-tenant NFR — no shared infrastructure or per-tenant routing logic anywhere.
- **CI/CD, container orchestration specifics (Kubernetes vs. simpler container hosting), and environment topology (dev/stage/prod)** are deferred to LLD/ops design — not blocking for HLD approval, but flagged as an implementation-phase decision.

---

## 11. Observability

- **Logging:** structured (JSON) application logs, correlation/request IDs propagated across module boundaries for traceability of a single request through Backlog → Sprint → Reporting → Notification event chains.
- **Metrics:** application and JVM metrics via Micrometer, scraped by Prometheus (or equivalent) — key metrics include API latency per endpoint, cache hit rate (Redis), DB connection pool saturation, and audit-write latency (since it's synchronous and on the critical path of every governance action).
- **Tracing:** distributed tracing (OpenTelemetry) even within the monolith, to trace a request across module boundaries and the async notification/reporting event paths.
- **Health checks:** liveness/readiness endpoints for the load balancer and orchestrator.

---

## 12. Availability & Disaster Recovery (Proposal — Needs Sign-off)

PRD §4 left this open. Proposed default, informed by the Large-scale/self-hosted single-tenant context — **flagged for explicit stakeholder confirmation before Database Design phase**, since backup/replication strategy affects DB topology decisions there:

- PostgreSQL: primary + at least one synchronous or near-synchronous standby for failover, plus the read replica(s) from §5 (which can double as failover candidates).
- Automated daily full backups + continuous WAL archiving, enabling point-in-time recovery.
- **Backups are encrypted at rest and replicated to an offsite/separate-region location** — a same-datacenter-only backup does not protect against a facility-level failure, which the initial draft left implicit rather than stated.
- **Backup verification and restore testing are scheduled, not assumed.** A backup that has never been restored is unverified; the DR plan includes a periodic (proposed: monthly) automated restore-to-scratch-environment test that confirms the backup is actually usable, not just that the backup job reported success.
- Target (proposed, not yet confirmed): RPO ≤ 15 minutes, RTO ≤ 1 hour. **Needs stakeholder confirmation.**
- Redis cache/pub-sub registry is treated as disposable/rebuildable (cache-only, no durable state that isn't also in PostgreSQL) — no DR requirement on Redis itself, though HA (Sentinel/Cluster, §10) still applies for availability during normal operation.

---

## 13. Non-Functional Performance & Operational Targets

Raised by external review as missing: measurable targets, not just architectural intent. Proposed defaults for the Large-scale target (§1) — **flagged for the same stakeholder confirmation as §12**, since these numbers should be signed off alongside DR targets before Database Design locks in indexing/connection-pool decisions built around them:

| Metric | Proposed Target |
|---|---|
| API latency (read endpoints) | P95 < 300 ms, P99 < 800 ms |
| API latency (write endpoints, incl. audit write) | P95 < 500 ms |
| Board load time (initial render) | < 1.5 s at 500 issues on-board |
| Report generation (burndown/velocity/CFD) | < 1 s (served from projections, §5/§8.2 — not computed live) |
| Search query latency | < 500 ms at v1 scale (Postgres FTS); re-evaluated if the §5 search-extraction trigger is hit |
| Real-time update propagation (§8.4) | < 2 s from server-side change to subscribed client |
| Max attachment size | 25 MB per file (proposed; confirm against actual usage patterns) |
| Concurrent WebSocket connections | Sized to concurrent-user target: 300+ simultaneous subscriptions, validated under load test |
| Redis cache hit ratio (board/permission cache) | > 90% in steady state |
| DB connection pool | Sized per app instance via HikariCP, ceiling derived from (max app instances × per-instance pool size) staying under PostgreSQL's max_connections with headroom for replicas/tooling — exact sizing finalized at Database Design once instance count is set |
| Scalability validation | Load test against the full Large-scale target (300+ concurrent users, 10k+ issues/project) before production go-live, not assumed from architecture alone |
| Security testing | Penetration test and dependency/SCA scan prior to go-live, and on a recurring cadence thereafter |
| Accessibility | WCAG 2.1 AA target for the SPA — carried forward as a requirement into UI Design phase |
| Browser support | Latest two versions of Chrome, Edge, Firefox, Safari — confirmed at UI Design phase |

---

## 14. Technology Stack Summary

| Layer | Choice |
|---|---|
| Backend | Java, Spring Boot (modular monolith) |
| Frontend | React + TypeScript SPA |
| Primary datastore | PostgreSQL (primary + standby + read replicas) |
| Cache / real-time registry | Redis (Sentinel or Cluster for HA) |
| Object storage | S3-compatible, optionally CDN-fronted |
| Reverse proxy / gateway | Nginx or Traefik (TLS termination, rate limiting, correlation IDs) |
| Real-time transport | WebSocket (STOMP), SSE fallback |
| Background jobs | In-process scheduled worker pool over a durable job table (§8.5) |
| Auth | Spring Security — local credentials + SAML2/OAuth2/OIDC |
| Messaging pattern | Domain event bus abstraction (§8.6): in-process application events + transactional outbox (no external broker required for v1) |
| Observability | Structured logs, Micrometer/Prometheus, OpenTelemetry |

**Note on messaging:** an external broker (e.g., RabbitMQ/Kafka) is deliberately **not** introduced in v1 — the transactional outbox pattern within the monolith is sufficient at the Large-scale target and avoids operating a separate messaging cluster. Because all cross-module coordination already flows through the single `DomainEventPublisher` abstraction (§8.6), swapping in a broker later is a localized change, not a rearchitecture — this is the explicit extraction point if a future module (most likely Notifications or Reporting) is split into its own service.

---

## 15. NFR Traceability

| PRD Requirement | HLD Realization |
|---|---|
| Single-tenant | One full-stack deployment per org; no tenant_id anywhere in schema or code (§10). |
| SSO (SAML/OAuth2/OIDC) | Spring Security federated auth, auto-link-by-email, §7. |
| API-layer RBAC | Shared `AuthorizationInterceptor` policy layer in front of every controller, §6. |
| Auditability | Synchronous, transactional, append-only audit log, §7, §8.3. |
| Large scale (300+ users, 100+ projects, 10k+ issues/project) | Stateless horizontally-scaled app tier + read replicas + Redis cache + projection-based reporting, §5, §10, measured against §13 targets. |
| Shared backlog, dual board views | Sprint & Board module reads/writes the same Backlog & Issues data; boards are views, not forks (§4). |
| One active sprint per project (v1) | Enforced in Sprint & Board module's sprint state machine (detailed at LLD). |
| Real-time board reflection (FR-24) | WebSocket/SSE push over the domain event bus, §8.4. |
| Configurable per-project workflow (FR-30) | Workflow engine sub-component within Sprint & Board, §4. |

---

## 16. Deferred Product Scope (External Review, 2026-07-13)

An external architecture review raised several capabilities common in enterprise platforms (Jira/Azure DevOps/ServiceNow-class): automation engine, plugin/marketplace architecture, third-party integrations and webhooks (GitHub/GitLab/Jenkins/Slack/Teams/etc.), field- and issue-level permission schemes beyond the approved project-role matrix, and enterprise modules such as time tracking, budgets, capacity/portfolio planning, roadmaps, and a wiki/document-management area.

**Stakeholder decision (2026-07-13): keep current PRD scope.** None of these are added to v1. This is recorded here rather than silently dropped because:
- They were explicitly marked out of scope in the approved PRD (§6: plugin/marketplace, advanced automation rule engines, billing/budget beyond basic time-logging, portfolio/program roll-up reporting, advanced test-management).
- Reopening them is a **product scope decision**, not an architecture decision — it belongs back at the PRD phase if priorities change, not absorbed into HLD without that review.
- The architecture in §4 (module boundaries), §8.6 (domain event bus abstraction), and §6 (REST API layer) was deliberately kept extensible toward these capabilities — e.g., an Automation module would subscribe to the same domain events Reporting/Notifications already consume; an Integration/Webhook module would be another outbound consumer of that same bus; a Search module sits behind the existing `IssueSearchService` interface (§5) — so choosing not to build them now does not foreclose adding them later without a rearchitecture.

If any of these become priorities, the correct next step is a PRD amendment (new functional requirements, updated out-of-scope list) followed by a corresponding HLD update — not a direct jump to LLD/implementation for that capability.

---

## 17. Out of Scope for This Phase

Deferred to later phases, not decided here:
- Database schema, table design, indexing details → **Database Design phase**.
- API endpoint contracts, request/response payloads, error model → **API Design phase**.
- Screen layouts, component inventory, design system → **UI Design phase**.
- Class-level design, sprint state machine detail, permission matrix data model, workflow-engine data model (§4) → **LLD**.
- CI/CD pipeline and environment topology specifics.
- Final Availability/DR targets (proposed in §12) and NFR performance targets (proposed in §13) — both need explicit sign-off.
- Capabilities listed in §16 — deferred pending a PRD scope decision, not designed here.

---

## 18. Approval & Next Steps

**This HLD is approved (2026-07-13).** The §12 Availability/DR proposal (primary+standby PostgreSQL, encrypted offsite backups, RPO ≤ 15 min / RTO ≤ 1 hour) and the §13 NFR performance/operational targets are accepted as proposed, with no amendments requested. Both remain **carried forward as inputs to Database Design** (they drive replication topology, indexing, and connection-pool sizing there) rather than re-litigated at this phase.

The next phase is **Low-Level Design (LLD)**: module-internal class/service design, the sprint and issue status-transition state machines, the workflow-engine data model (§4), the permission-matrix data model, and event-flow detail for the notification/reporting/real-time/background-job consumers of the domain event bus described in §8. No implementation code will be written before LLD and subsequent Database Design, API Design, and UI Design phases are each reviewed in turn.

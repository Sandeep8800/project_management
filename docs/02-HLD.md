# Nexus PMS — High-Level Design (HLD)

**Status:** Draft for review
**Phase:** 2 of 7 — Documentation-first sequence (PRD → **HLD** → LLD → Database Design → API Design → UI Design → Coding)
**Input:** [`01-PRD.md`](./01-PRD.md) (Approved, 2026-07-13)
**Owners:** Architecture
**Last updated:** 2026-07-13

> This document defines system architecture, service boundaries, and technology choices. It does not define database schemas (Database Design phase), API contracts (API Design phase), UI layouts (UI Design phase), or class/module-internal design (LLD). No implementation code is written against this phase.

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
| **Sprint & Board** | Sprints, board configuration (Scrum/Kanban), WIP limits, workflow/status definitions | Backlog & Issues, Governance & RBAC |
| **Reporting** | Burndown, velocity, CFD, sprint summary (read-optimized projections) | Sprint & Board, Backlog & Issues (read-only, via projections not live joins) |
| **Notifications** | In-app notifications, email dispatch, delivery preferences | Identity & Access, event feed from all modules |

This mirrors PRD §3's grouping (Admin, Backlog, Sprint, Board, Reporting, Notifications) directly, so requirements traceability from PRD → module is 1:1.

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
- **Search:** PostgreSQL full-text search (`tsvector`) for issue search/filtering in v1. Flagged for revisit at DB Design phase if query patterns at full scale (1M+ issues) demand a dedicated search engine (e.g., OpenSearch) — not built in v1 to avoid premature infrastructure.

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

---

## 9. Frontend Architecture

- **React + TypeScript SPA**, structured by the same module boundaries as the backend (Admin Console, Backlog, Board, Sprint, Reports, Auth) to keep frontend/backend mental models aligned.
- **Server state** (issues, boards, sprints, reports) managed via a query/cache library (e.g., React Query) — not global Redux state — since almost all app state is server-derived and needs cache invalidation semantics, not client-owned state.
- **UI-level permission hints** (hiding/disabling buttons a user's role can't use) are a pure UX convenience layer that mirrors the server's permission matrix for responsiveness — they are never the enforcement point (§6, §7). Every mutating action still round-trips through server-side authorization.
- Full component/screen inventory deferred to the UI Design phase.

---

## 10. Deployment Architecture

- **App tier:** containerized Spring Boot instances, stateless, horizontally scaled behind a load balancer. No session affinity required (JWT-based auth, §6).
- **Data tier:** PostgreSQL primary + read replica(s); Redis (managed or self-hosted) for cache; S3-compatible object storage for attachments.
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
- Target (proposed, not yet confirmed): RPO ≤ 15 minutes, RTO ≤ 1 hour. **Needs stakeholder confirmation.**
- Redis cache is treated as disposable/rebuildable (cache-only, no durable state that isn't also in PostgreSQL) — no DR requirement on Redis itself.

---

## 13. Technology Stack Summary

| Layer | Choice |
|---|---|
| Backend | Java, Spring Boot (modular monolith) |
| Frontend | React + TypeScript SPA |
| Primary datastore | PostgreSQL (primary + read replicas) |
| Cache | Redis |
| Object storage | S3-compatible |
| Auth | Spring Security — local credentials + SAML2/OAuth2/OIDC |
| Messaging pattern | In-process application events + transactional outbox (no external broker required for v1) |
| Observability | Structured logs, Micrometer/Prometheus, OpenTelemetry |

**Note on messaging:** an external broker (e.g., RabbitMQ/Kafka) is deliberately **not** introduced in v1 — the transactional outbox pattern within the monolith is sufficient at the Large-scale target and avoids operating a separate messaging cluster. This is an explicit extraction point if a future module (most likely Notifications or Reporting) is later split into its own service.

---

## 14. NFR Traceability

| PRD Requirement | HLD Realization |
|---|---|
| Single-tenant | One full-stack deployment per org; no tenant_id anywhere in schema or code (§10). |
| SSO (SAML/OAuth2/OIDC) | Spring Security federated auth, auto-link-by-email, §7. |
| API-layer RBAC | Shared `AuthorizationInterceptor` policy layer in front of every controller, §6. |
| Auditability | Synchronous, transactional, append-only audit log, §7, §8.3. |
| Large scale (300+ users, 100+ projects, 10k+ issues/project) | Stateless horizontally-scaled app tier + read replicas + Redis cache + projection-based reporting, §5, §10. |
| Shared backlog, dual board views | Sprint & Board module reads/writes the same Backlog & Issues data; boards are views, not forks (§4). |
| One active sprint per project (v1) | Enforced in Sprint & Board module's sprint state machine (detailed at LLD). |

---

## 15. Out of Scope for This Phase

Deferred to later phases, not decided here:
- Database schema, table design, indexing details → **Database Design phase**.
- API endpoint contracts, request/response payloads, error model → **API Design phase**.
- Screen layouts, component inventory, design system → **UI Design phase**.
- Class-level design, sprint state machine detail, permission matrix data model → **LLD**.
- CI/CD pipeline and environment topology specifics.
- Final Availability/DR targets (proposed in §12, needs explicit sign-off).

---

## 16. Approval & Next Steps

This HLD is **not final** until:
1. The Availability/DR proposal in §12 is confirmed or amended by stakeholders.
2. This document is explicitly reviewed and approved.

Upon approval, the next phase is **Low-Level Design (LLD)**: module-internal class/service design, the sprint and issue status-transition state machines, the permission-matrix data model, and event-flow detail for the notification/reporting projections described in §8. No implementation code will be written before LLD and subsequent Database Design, API Design, and UI Design phases are each reviewed in turn.

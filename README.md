# Nexus PMS

An Agile/Scrum + Kanban project management system, built admin-governed (no self-service
signup) with project-scoped RBAC. See [`docs/`](./docs) for the full documentation-first
design chain (PRD → HLD → LLD → Database Design → API Design → UI Design), all approved.

## Structure

- `backend/` — Spring Boot modular monolith (Java 21, Maven). See `docs/02-HLD.md` §4 for
  module boundaries and `docs/03-LLD.md` for internal design.
- `frontend/` — React + TypeScript SPA (Vite). See `docs/06-ui-design.md` for screen inventory.

## Implementation status

**Fully implemented, backend and frontend, end to end:**
- **Identity & Access** — local + SSO-assertion auth (auto-link-by-email), JWT
  issuance/refresh rotation, admin user provisioning, per-user email-notification opt-in.
- **Governance & RBAC** — admin project/membership management, project-scoped permission
  resolution (`PermissionResolver` + `AuthorizationAspect`), synchronous audit logging.
- **Backlog & Issues** — full issue CRUD across all five types, optimistic locking,
  backlog reorder, comments, issue links, labels, attachment metadata (upload backend
  stubbed — no real object storage provisioned).
- **Sprint & Board** — sprint lifecycle state machine, per-project workflow engine,
  Scrum/Kanban boards over the shared backlog, Kanban WIP-limit enforcement.
- **Reporting** — burndown/velocity/CFD/sprint-summary projections, event-driven
  (recompute-on-event, not a true incremental delta — see `ProjectionUpdateService`
  javadoc for the documented gap vs. the HLD's target design).
- **Notifications** — transactional outbox, in-app delivery (real), email delivery
  (stubbed — logs instead of sending, no SMTP provider provisioned).
- **Real-time** — STOMP/WebSocket board and notification push, single-instance only
  (no Redis-backed multi-instance registry; see `WebSocketConfig` javadoc).

**Known gaps / follow-up work**, each flagged in code comments at the relevant class:
- No real object storage or email provider wired — both are stubbed.
- Reporting projections recompute in full on each event rather than applying a true
  incremental delta (correct, not yet the target-scale-sized implementation).
- `REMINDER` background job type has a handler but nothing schedules one yet.
- Real-time push doesn't survive running more than one app instance.
- Drag-and-drop on the Kanban/Scrum board doesn't pre-validate against the workflow
  graph client-side the way the Issue Detail panel's status dropdown does — an illegal
  drop surfaces as a server-side error rather than being blocked before the request.

## Running locally

### Backend
Requires PostgreSQL. Flyway migrations run automatically on startup.

```
cd backend
DB_URL=jdbc:postgresql://localhost:5432/nexuspms \
DB_USERNAME=nexuspms DB_PASSWORD=nexuspms \
NEXUS_BOOTSTRAP_ADMIN_EMAIL=admin@example.com \
NEXUS_BOOTSTRAP_ADMIN_PASSWORD=change-me-immediately \
mvn spring-boot:run
```

The bootstrap admin env vars provision exactly one platform Admin on first startup (see
`BootstrapAdminRunner`) — the one deliberate exception to "no self-service," since an
admin-governed system otherwise has no way to create its first admin. Omit them after
first deploy.

### Frontend
```
cd frontend
npm install
npm run dev
```
Dev server proxies `/api/v1` to `http://localhost:8080` (see `vite.config.ts`).

### Tests
```
cd backend && mvn test
```

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
  backlog reorder, comments, issue links, labels, attachments via a local-disk object
  storage stand-in (`LocalObjectStorageController`, path-traversal-safe; swap for real
  S3 by replacing that one class + `AttachmentService.generateUploadUrl`).
- **Sprint & Board** — sprint lifecycle state machine, per-project workflow engine,
  Scrum/Kanban boards over the shared backlog, Kanban WIP-limit enforcement, a daily
  scheduler that enqueues sprint-ending-soon reminders (`SprintReminderScheduler`).
- **Reporting** — burndown/velocity/CFD/sprint-summary projections. CFD updates are a
  true incremental delta per event (carry-forward-seeded, see `ProjectionUpdateService`);
  burndown is a targeted per-sprint recompute, bounded by sprint size, not project size.
- **Notifications** — transactional outbox, in-app delivery (real), email delivery via
  real SMTP (`JavaMailSender`, gated by `nexus.mail.enabled` — logs instead of sending
  when disabled/unconfigured, which is the default since no SMTP server is provisioned
  in this environment). The `REMINDER` job type is scheduled end-to-end (Sprint & Board
  enqueues, Notifications delivers).
- **Real-time** — STOMP/WebSocket board and notification push, illegal Kanban/Scrum
  drag-drop targets are blocked client-side using the fetched workflow graph before a
  request is even sent (server-side validation remains the actual authority).

**Known gaps / follow-up work**, each flagged in code comments at the relevant class:
- Real-time push doesn't survive running more than one app instance (no Redis-backed
  subscription registry — see `WebSocketConfig` javadoc).
- Issue-key generation (`{PROJECT}-{n}`) isn't race-safe under truly concurrent creates
  on the same project across multiple app instances (see `IssueKeyGenerator`).
- The local-disk storage stand-in has no per-issue authorization check on the storage
  endpoint itself (relies on the storage key's random UUID component being unguessable,
  the same posture a real pre-signed URL has — see `LocalObjectStorageController`).

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

Optional env vars: `NEXUS_STORAGE_PATH` (local-disk attachment storage, default
`./data/attachments`), `NEXUS_STORAGE_BASE_URL` (default `http://localhost:8080/api/v1`),
`NEXUS_MAIL_ENABLED` + `NEXUS_SMTP_HOST`/`_PORT`/`_USERNAME`/`_PASSWORD` (real email
delivery, off by default — logs instead of sending when unset).

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

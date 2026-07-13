# Nexus PMS

An Agile/Scrum + Kanban project management system, built admin-governed (no self-service
signup) with project-scoped RBAC. See [`docs/`](./docs) for the full documentation-first
design chain (PRD → HLD → LLD → Database Design → API Design → UI Design), all approved.

## Structure

- `backend/` — Spring Boot modular monolith (Java 21, Maven). See `docs/02-HLD.md` §4 for
  module boundaries and `docs/03-LLD.md` for internal design.
- `frontend/` — React + TypeScript SPA (Vite). See `docs/06-ui-design.md` for screen inventory.

## Implementation status

**Fully implemented:** Identity & Access and Governance & RBAC (auth, JWT, project-scoped
permission resolution, admin user/project/membership management, audit logging) — backend
and frontend, end to end.

**Scaffolded, not implemented:** Backlog & Issues, Sprint & Board (incl. Workflow Engine),
Reporting, Notifications. Package structure and design docs exist (see each module's
`package-info.java` under `backend/src/main/java/com/nexuspms/`); business logic is
follow-up work.

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

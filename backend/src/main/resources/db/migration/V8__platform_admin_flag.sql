-- Fixes a bootstrap gap found during Coding: platform-wide Admin (HLD S1, "the one
-- role with platform-wide scope") was originally going to be derived from holding
-- an ADMIN project_memberships row -- but creating a project itself requires an
-- Admin, so the very first admin account could never be provisioned. This flag is
-- a user-level attribute, independent of any project assignment, so the system is
-- actually usable after first deploy without a self-service backdoor.
ALTER TABLE users ADD COLUMN platform_admin boolean NOT NULL DEFAULT false;
CREATE INDEX idx_users_platform_admin ON users (platform_admin) WHERE platform_admin = true;

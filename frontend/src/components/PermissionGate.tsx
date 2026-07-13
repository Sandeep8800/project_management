import type { ReactNode } from "react";
import { useAuth } from "../features/auth/AuthContext";

interface PermissionGateProps {
  requireAdmin?: boolean;
  children: ReactNode;
  fallback?: ReactNode;
}

/**
 * UI Design S7: the single mechanism for role-based UI variation -- no screen
 * independently reimplements "if role is X, show Y." This is a UX convenience
 * only (UI Design Principle 1); every mutating action still round-trips through
 * the server's own authorization check regardless of what this renders.
 *
 * Scope note: only the platform-Admin gate is wired in this pass, since
 * project-scoped permission resolution depends on Backlog/Sprint/Board module
 * endpoints that are stubbed, not implemented, in this Coding pass (see
 * backend backlog/sprintboard package-info.java). Follow-up work: fetch the
 * resolved per-project permission set (API Design future endpoint) and gate on
 * specific Permission values, per UI Design S6.
 */
export function PermissionGate({ requireAdmin, children, fallback = null }: PermissionGateProps) {
  const { user } = useAuth();
  if (requireAdmin && !user?.platformAdmin) {
    return <>{fallback}</>;
  }
  return <>{children}</>;
}

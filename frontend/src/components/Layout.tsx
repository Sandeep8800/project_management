import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../features/auth/AuthContext";
import { PermissionGate } from "./PermissionGate";

// UI Design S3: top-level nav shell -- Admin Console only renders if the caller
// is platform Admin; Backlog/Board/Sprints/Reports are follow-up work (their
// backend modules are stubbed, not implemented, in this pass).
export function Layout() {
  const { user, logout } = useAuth();

  return (
    <div className="app-shell">
      <header className="app-header">
        <span className="app-title">Nexus PMS</span>
        <nav>
          <NavLink to="/">Home</NavLink>
          <PermissionGate requireAdmin>
            <NavLink to="/admin">Admin Console</NavLink>
          </PermissionGate>
        </nav>
        <div className="app-account">
          <span>{user?.name}</span>
          <button onClick={() => logout()}>Sign out</button>
        </div>
      </header>
      <main className="app-content">
        <Outlet />
      </main>
    </div>
  );
}

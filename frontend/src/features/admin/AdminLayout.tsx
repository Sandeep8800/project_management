import { NavLink, Outlet } from "react-router-dom";

// UI Design S4.2: Admin Console is a fully separate section, not buttons
// scattered across project screens.
export function AdminLayout() {
  return (
    <div className="admin-layout">
      <nav className="admin-subnav">
        <NavLink to="/admin" end>
          Dashboard
        </NavLink>
        <NavLink to="/admin/users">Users</NavLink>
        <NavLink to="/admin/projects">Projects</NavLink>
        <NavLink to="/admin/audit-log">Audit Log</NavLink>
      </nav>
      <div className="admin-content">
        <Outlet />
      </div>
    </div>
  );
}

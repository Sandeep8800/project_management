import { Navigate, Route, Routes } from "react-router-dom";
import { Layout } from "./components/Layout";
import { ProtectedRoute } from "./components/ProtectedRoute";
import { PermissionGate } from "./components/PermissionGate";
import { LoginPage } from "./features/auth/LoginPage";
import { HomePage } from "./features/home/HomePage";
import { AdminLayout } from "./features/admin/AdminLayout";
import { DashboardPage } from "./features/admin/DashboardPage";
import { UsersPage } from "./features/admin/UsersPage";
import { ProjectsPage } from "./features/admin/ProjectsPage";
import { AuditLogPage } from "./features/admin/AuditLogPage";

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        element={
          <ProtectedRoute>
            <Layout />
          </ProtectedRoute>
        }
      >
        <Route index element={<HomePage />} />
        <Route
          path="admin"
          element={
            <PermissionGate requireAdmin fallback={<Navigate to="/" replace />}>
              <AdminLayout />
            </PermissionGate>
          }
        >
          <Route index element={<DashboardPage />} />
          <Route path="users" element={<UsersPage />} />
          <Route path="projects" element={<ProjectsPage />} />
          <Route path="audit-log" element={<AuditLogPage />} />
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

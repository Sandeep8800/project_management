import { apiRequest } from "../../lib/apiClient";

export interface PageResponse<T> {
  data: T[];
  pagination: { page?: number; size?: number; totalElements?: number; nextCursor?: string; hasMore: boolean };
}

export interface AdminUser {
  id: string;
  name: string;
  email: string;
  employeeId?: string;
  department?: string;
  defaultRole: string;
  status: "ACTIVE" | "DEACTIVATED";
  platformAdmin: boolean;
  createdAt: string;
}

export interface AdminProject {
  id: string;
  projectKey: string;
  name: string;
  description?: string;
  methodology: "SCRUM" | "KANBAN" | "HYBRID";
  startDate: string;
  targetReleaseDate?: string;
  status: "ACTIVE" | "ARCHIVED" | "DELETED";
}

export interface Membership {
  userId: string;
  projectId: string;
  role: string;
  assignedAt: string;
}

export interface DashboardSummary {
  activeProjectCount: number;
  archivedProjectCount: number;
  totalUserCount: number;
}

export interface AuditLogEntry {
  id: string;
  actorId: string;
  actionType: string;
  targetEntityType: string;
  targetEntityId: string;
  metadata?: Record<string, unknown>;
  createdAt: string;
}

// -- Users (PRD FR-1..5, API Design S5) --
export const listUsers = (params: URLSearchParams) => apiRequest<PageResponse<AdminUser>>(`/admin/users?${params}`);
export const createUser = (body: {
  name: string;
  email: string;
  employeeId?: string;
  department?: string;
  defaultRole: string;
  initialPassword?: string;
}) => apiRequest<AdminUser>("/admin/users", { method: "POST", body });
export const deactivateUser = (id: string) => apiRequest<void>(`/admin/users/${id}/deactivate`, { method: "POST" });
export const reactivateUser = (id: string) => apiRequest<void>(`/admin/users/${id}/reactivate`, { method: "POST" });
export const resetPassword = (id: string, newPassword: string) =>
  apiRequest<void>(`/admin/users/${id}/reset-password`, { method: "POST", body: { newPassword } });
export const grantAdmin = (id: string) => apiRequest<void>(`/admin/users/${id}/grant-admin`, { method: "POST" });
export const revokeAdmin = (id: string) => apiRequest<void>(`/admin/users/${id}/revoke-admin`, { method: "POST" });

// -- Projects (PRD FR-6..9, API Design S5) --
export const listProjects = (params: URLSearchParams) =>
  apiRequest<PageResponse<AdminProject>>(`/admin/projects?${params}`);
export const createProject = (body: {
  projectKey: string;
  name: string;
  description?: string;
  methodology: string;
  startDate: string;
  targetReleaseDate?: string;
}) => apiRequest<AdminProject>("/admin/projects", { method: "POST", body });
export const archiveProject = (id: string) => apiRequest<void>(`/admin/projects/${id}/archive`, { method: "POST" });
export const deleteProject = (id: string) => apiRequest<void>(`/admin/projects/${id}`, { method: "DELETE" });

// -- Memberships (PRD FR-10/11/14, API Design S5) --
export const listMemberships = (projectId: string) => apiRequest<Membership[]>(`/admin/projects/${projectId}/memberships`);
export const assignMembership = (projectId: string, userId: string, role: string) =>
  apiRequest<Membership>(`/admin/projects/${projectId}/memberships`, { method: "POST", body: { userId, role } });
export const changeMembershipRole = (projectId: string, userId: string, role: string) =>
  apiRequest<Membership>(`/admin/projects/${projectId}/memberships/${userId}`, { method: "PATCH", body: { role } });
export const removeMembership = (projectId: string, userId: string) =>
  apiRequest<void>(`/admin/projects/${projectId}/memberships/${userId}`, { method: "DELETE" });

// -- Dashboard & Audit Log (PRD FR-16/17, API Design S5) --
export const fetchDashboard = () => apiRequest<DashboardSummary>("/admin/dashboard");
export const searchAuditLog = (params: URLSearchParams) =>
  apiRequest<PageResponse<AuditLogEntry>>(`/admin/audit-log?${params}`);

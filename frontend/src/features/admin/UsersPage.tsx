import { useState, type FormEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  createUser,
  deactivateUser,
  grantAdmin,
  listUsers,
  reactivateUser,
  revokeAdmin,
  type AdminUser,
} from "./adminApi";
import { ApiError } from "../../lib/apiClient";

// UI Design S4.2 User Management screen. PRD FR-1..5: admin-only provisioning,
// no self-service anywhere -- there is no public equivalent of this form.
export function UsersPage() {
  const queryClient = useQueryClient();
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: ["admin", "users"],
    queryFn: () => listUsers(new URLSearchParams({ size: "50" })),
  });

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ["admin", "users"] });

  const createMutation = useMutation({
    mutationFn: createUser,
    onSuccess: () => {
      invalidate();
      setShowCreateForm(false);
    },
    onError: (err) => setError(err instanceof ApiError ? err.message : "Failed to create user."),
  });

  const deactivateMutation = useMutation({ mutationFn: deactivateUser, onSuccess: invalidate });
  const reactivateMutation = useMutation({ mutationFn: reactivateUser, onSuccess: invalidate });
  const grantAdminMutation = useMutation({ mutationFn: grantAdmin, onSuccess: invalidate });
  const revokeAdminMutation = useMutation({ mutationFn: revokeAdmin, onSuccess: invalidate });

  function handleCreate(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setError(null);
    const form = new FormData(e.currentTarget);
    createMutation.mutate({
      name: String(form.get("name")),
      email: String(form.get("email")),
      employeeId: String(form.get("employeeId") || "") || undefined,
      department: String(form.get("department") || "") || undefined,
      defaultRole: String(form.get("defaultRole")),
      initialPassword: String(form.get("initialPassword") || "") || undefined,
    });
  }

  return (
    <div>
      <div className="page-header">
        <h2>User Management</h2>
        <button onClick={() => setShowCreateForm((s) => !s)}>{showCreateForm ? "Cancel" : "Create User"}</button>
      </div>

      {showCreateForm && (
        <form onSubmit={handleCreate} className="inline-form">
          <input name="name" placeholder="Full name" required />
          <input name="email" type="email" placeholder="Email" required />
          <input name="employeeId" placeholder="Employee ID (optional)" />
          <input name="department" placeholder="Department (optional)" />
          <select name="defaultRole" defaultValue="DEVELOPER">
            <option value="PROJECT_MANAGER_SCRUM_MASTER">Project Manager / Scrum Master</option>
            <option value="PRODUCT_OWNER">Product Owner</option>
            <option value="DEVELOPER">Developer</option>
            <option value="QA_TESTER">QA / Tester</option>
            <option value="VIEWER_STAKEHOLDER">Viewer / Stakeholder</option>
          </select>
          <input name="initialPassword" type="password" placeholder="Initial password (optional -- SSO-only if blank)" />
          {error && <p className="form-error">{error}</p>}
          <button type="submit" disabled={createMutation.isPending}>
            {createMutation.isPending ? "Creating..." : "Create"}
          </button>
        </form>
      )}

      {isLoading ? (
        <p>Loading...</p>
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Email</th>
              <th>Department</th>
              <th>Default Role</th>
              <th>Status</th>
              <th>Platform Admin</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {data?.data.map((user: AdminUser) => (
              <tr key={user.id}>
                <td>{user.name}</td>
                <td>{user.email}</td>
                <td>{user.department ?? "—"}</td>
                <td>{user.defaultRole}</td>
                <td>{user.status}</td>
                <td>{user.platformAdmin ? "Yes" : "No"}</td>
                <td className="row-actions">
                  {user.status === "ACTIVE" ? (
                    <button onClick={() => deactivateMutation.mutate(user.id)}>Deactivate</button>
                  ) : (
                    <button onClick={() => reactivateMutation.mutate(user.id)}>Reactivate</button>
                  )}
                  {user.platformAdmin ? (
                    <button onClick={() => revokeAdminMutation.mutate(user.id)}>Revoke Admin</button>
                  ) : (
                    <button onClick={() => grantAdminMutation.mutate(user.id)}>Grant Admin</button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}

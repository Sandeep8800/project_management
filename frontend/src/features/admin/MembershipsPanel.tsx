import { useState, type FormEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { assignMembership, changeMembershipRole, listMemberships, listUsers, removeMembership } from "./adminApi";
import { ApiError } from "../../lib/apiClient";

const ROLES = [
  "ADMIN",
  "PROJECT_MANAGER_SCRUM_MASTER",
  "PRODUCT_OWNER",
  "DEVELOPER",
  "QA_TESTER",
  "VIEWER_STAKEHOLDER",
];

// UI Design S4.2 Project Memberships screen. PRD FR-10/11: add-user-to-project
// searches EXISTING users only -- never an inline "create new user" shortcut,
// keeping user creation and project assignment as distinct governed actions.
export function MembershipsPanel({ projectId }: { projectId: string }) {
  const queryClient = useQueryClient();
  const [error, setError] = useState<string | null>(null);

  const { data: memberships, isLoading } = useQuery({
    queryKey: ["admin", "projects", projectId, "memberships"],
    queryFn: () => listMemberships(projectId),
  });
  const { data: allUsers } = useQuery({
    queryKey: ["admin", "users", "all-active"],
    queryFn: () => listUsers(new URLSearchParams({ status: "ACTIVE", size: "100" })),
  });

  const invalidate = () =>
    queryClient.invalidateQueries({ queryKey: ["admin", "projects", projectId, "memberships"] });

  const assignMutation = useMutation({
    mutationFn: ({ userId, role }: { userId: string; role: string }) => assignMembership(projectId, userId, role),
    onSuccess: invalidate,
    onError: (err) => setError(err instanceof ApiError ? err.message : "Failed to assign membership."),
  });
  const changeRoleMutation = useMutation({
    mutationFn: ({ userId, role }: { userId: string; role: string }) => changeMembershipRole(projectId, userId, role),
    onSuccess: invalidate,
  });
  const removeMutation = useMutation({
    mutationFn: (userId: string) => removeMembership(projectId, userId),
    onSuccess: invalidate,
  });

  function handleAssign(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setError(null);
    const form = new FormData(e.currentTarget);
    assignMutation.mutate({ userId: String(form.get("userId")), role: String(form.get("role")) });
    e.currentTarget.reset();
  }

  const userNameById = new Map((allUsers?.data ?? []).map((u) => [u.id, `${u.name} (${u.email})`]));

  return (
    <div className="memberships-panel">
      <form onSubmit={handleAssign} className="inline-form">
        <select name="userId" required defaultValue="">
          <option value="" disabled>
            Select existing user...
          </option>
          {allUsers?.data.map((u) => (
            <option key={u.id} value={u.id}>
              {u.name} ({u.email})
            </option>
          ))}
        </select>
        <select name="role" defaultValue="DEVELOPER">
          {ROLES.map((r) => (
            <option key={r} value={r}>
              {r}
            </option>
          ))}
        </select>
        <button type="submit" disabled={assignMutation.isPending}>
          Add to Project
        </button>
      </form>
      {error && <p className="form-error">{error}</p>}

      {isLoading ? (
        <p>Loading memberships...</p>
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              <th>User</th>
              <th>Role</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {memberships?.map((m) => (
              <tr key={m.userId}>
                <td>{userNameById.get(m.userId) ?? m.userId}</td>
                <td>
                  <select
                    value={m.role}
                    onChange={(e) => changeRoleMutation.mutate({ userId: m.userId, role: e.target.value })}
                  >
                    {ROLES.map((r) => (
                      <option key={r} value={r}>
                        {r}
                      </option>
                    ))}
                  </select>
                </td>
                <td>
                  <button onClick={() => removeMutation.mutate(m.userId)}>Remove</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}

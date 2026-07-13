import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { searchAuditLog } from "./adminApi";

// UI Design S4.2 Audit Log: filterable, read-only, no export/edit affordance
// beyond viewing (PRD FR-17 requires queryable, not exportable, in v1).
export function AuditLogPage() {
  const [actionType, setActionType] = useState("");
  const [targetEntityType, setTargetEntityType] = useState("");

  const params = new URLSearchParams({ size: "50" });
  if (actionType) params.set("actionType", actionType);
  if (targetEntityType) params.set("targetEntityType", targetEntityType);

  const { data, isLoading } = useQuery({
    queryKey: ["admin", "audit-log", actionType, targetEntityType],
    queryFn: () => searchAuditLog(params),
  });

  return (
    <div>
      <h2>Audit Log</h2>
      <div className="inline-form">
        <select value={actionType} onChange={(e) => setActionType(e.target.value)}>
          <option value="">All action types</option>
          <option value="USER_CREATED">USER_CREATED</option>
          <option value="USER_DEACTIVATED">USER_DEACTIVATED</option>
          <option value="USER_REACTIVATED">USER_REACTIVATED</option>
          <option value="PLATFORM_ADMIN_GRANTED">PLATFORM_ADMIN_GRANTED</option>
          <option value="PLATFORM_ADMIN_REVOKED">PLATFORM_ADMIN_REVOKED</option>
          <option value="PROJECT_CREATED">PROJECT_CREATED</option>
          <option value="PROJECT_ARCHIVED">PROJECT_ARCHIVED</option>
          <option value="PROJECT_DELETED">PROJECT_DELETED</option>
          <option value="ROLE_ASSIGNED">ROLE_ASSIGNED</option>
          <option value="ROLE_CHANGED">ROLE_CHANGED</option>
          <option value="MEMBERSHIP_REMOVED">MEMBERSHIP_REMOVED</option>
        </select>
        <select value={targetEntityType} onChange={(e) => setTargetEntityType(e.target.value)}>
          <option value="">All target types</option>
          <option value="USER">USER</option>
          <option value="PROJECT">PROJECT</option>
          <option value="PROJECT_MEMBERSHIP">PROJECT_MEMBERSHIP</option>
        </select>
      </div>

      {isLoading ? (
        <p>Loading...</p>
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              <th>When</th>
              <th>Actor</th>
              <th>Action</th>
              <th>Target</th>
              <th>Details</th>
            </tr>
          </thead>
          <tbody>
            {data?.data.map((entry) => (
              <tr key={entry.id}>
                <td>{new Date(entry.createdAt).toLocaleString()}</td>
                <td>{entry.actorId}</td>
                <td>{entry.actionType}</td>
                <td>
                  {entry.targetEntityType}: {entry.targetEntityId}
                </td>
                <td>{entry.metadata ? JSON.stringify(entry.metadata) : "—"}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}

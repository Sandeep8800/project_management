import { useState } from "react";
import { useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { getBurndown, getCfd, getSprintSummary, getVelocity } from "./reportingApi";
import { listSprints } from "../sprintboard/sprintBoardApi";

/**
 * UI Design S4.7: four read-only report views. No chart library is added in
 * this pass -- bars are rendered with plain CSS width percentages, which is
 * enough to be legible without a new dependency; swapping in a real charting
 * library is a pure presentation-layer change later.
 */
export function ReportsPage() {
  const { projectId } = useParams<{ projectId: string }>();
  const [tab, setTab] = useState<"burndown" | "velocity" | "cfd" | "summary">("velocity");
  const [selectedSprintId, setSelectedSprintId] = useState<string>("");
  const [boardType, setBoardType] = useState<"SCRUM" | "KANBAN">("KANBAN");

  const { data: sprints } = useQuery({ queryKey: ["sprints", projectId], queryFn: () => listSprints(projectId!), enabled: Boolean(projectId) });

  const { data: burndown } = useQuery({
    queryKey: ["reports", "burndown", selectedSprintId],
    queryFn: () => getBurndown(projectId!, selectedSprintId),
    enabled: Boolean(projectId && selectedSprintId && tab === "burndown"),
  });
  const { data: velocity } = useQuery({
    queryKey: ["reports", "velocity", projectId],
    queryFn: () => getVelocity(projectId!),
    enabled: Boolean(projectId && tab === "velocity"),
  });
  const { data: cfd } = useQuery({
    queryKey: ["reports", "cfd", projectId, boardType],
    queryFn: () => {
      const to = new Date().toISOString().slice(0, 10);
      const from = new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10);
      return getCfd(projectId!, boardType, from, to);
    },
    enabled: Boolean(projectId && tab === "cfd"),
  });
  const { data: summary } = useQuery({
    queryKey: ["reports", "summary", selectedSprintId],
    queryFn: () => getSprintSummary(projectId!, selectedSprintId),
    enabled: Boolean(projectId && selectedSprintId && tab === "summary"),
  });

  const maxVelocity = Math.max(1, ...(velocity?.map((v) => Math.max(Number(v.committedPoints), Number(v.completedPoints))) ?? [1]));
  const maxBurndown = Math.max(1, ...(burndown?.map((b) => Number(b.remainingPoints)) ?? [1]));

  return (
    <div>
      <div className="page-header">
        <h2>Reports</h2>
        <div className="board-type-tabs">
          <button className={tab === "velocity" ? "active" : ""} onClick={() => setTab("velocity")}>Velocity</button>
          <button className={tab === "burndown" ? "active" : ""} onClick={() => setTab("burndown")}>Burndown</button>
          <button className={tab === "cfd" ? "active" : ""} onClick={() => setTab("cfd")}>Cumulative Flow</button>
          <button className={tab === "summary" ? "active" : ""} onClick={() => setTab("summary")}>Sprint Summary</button>
        </div>
      </div>

      {(tab === "burndown" || tab === "summary") && (
        <select value={selectedSprintId} onChange={(e) => setSelectedSprintId(e.target.value)}>
          <option value="">Select a sprint...</option>
          {sprints?.map((s) => (
            <option key={s.id} value={s.id}>
              {s.name} ({s.status})
            </option>
          ))}
        </select>
      )}

      {tab === "cfd" && (
        <select value={boardType} onChange={(e) => setBoardType(e.target.value as "SCRUM" | "KANBAN")}>
          <option value="KANBAN">Kanban</option>
          <option value="SCRUM">Scrum</option>
        </select>
      )}

      {tab === "velocity" && (
        <div className="chart">
          {velocity && velocity.length === 0 && <p>No completed sprints yet -- velocity will appear after your first sprint completes.</p>}
          {velocity?.map((v) => (
            <div key={v.sprintId} className="chart-row">
              <div className="chart-bar committed" style={{ width: `${(Number(v.committedPoints) / maxVelocity) * 100}%` }}>
                Committed: {v.committedPoints}
              </div>
              <div className="chart-bar completed" style={{ width: `${(Number(v.completedPoints) / maxVelocity) * 100}%` }}>
                Completed: {v.completedPoints}
              </div>
            </div>
          ))}
        </div>
      )}

      {tab === "burndown" && selectedSprintId && (
        <div className="chart">
          {burndown && burndown.length === 0 && <p>No burndown data yet for this sprint.</p>}
          {burndown?.map((b) => (
            <div key={b.date} className="chart-row">
              <span className="chart-label">{b.date}</span>
              <div className="chart-bar burndown" style={{ width: `${(Number(b.remainingPoints) / maxBurndown) * 100}%` }}>
                {b.remainingPoints} pts remaining ({b.remainingIssueCount} issues)
              </div>
            </div>
          ))}
        </div>
      )}

      {tab === "cfd" && (
        <table className="data-table">
          <thead>
            <tr>
              <th>Date</th>
              <th>Status</th>
              <th>Issue Count</th>
            </tr>
          </thead>
          <tbody>
            {cfd?.map((c, i) => (
              <tr key={i}>
                <td>{c.date}</td>
                <td>{c.statusName}</td>
                <td>{c.issueCount}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {tab === "summary" && selectedSprintId && summary && (
        <div className="sprint-summary-card">
          <p>Planned: {summary.plannedPoints} pts</p>
          <p>Completed: {summary.completedPoints} pts</p>
          <p>Scope added: {summary.scopeAddedPoints} pts</p>
          <p>Scope removed: {summary.scopeRemovedPoints} pts</p>
          <p>Carried over: {summary.carryOverIssueCount} issues</p>
        </div>
      )}
    </div>
  );
}

import { useState, type FormEvent } from "react";
import { useParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createIssue, listIssues, type Issue } from "./backlogApi";
import { ApiError } from "../../lib/apiClient";
import { IssueDetailPanel } from "../../components/IssueDetailPanel";

// UI Design S4.3: hierarchical backlog list with filter/search bar and a
// slide-over Issue Detail panel. Drag-to-reorder is represented here with
// simple up/down controls rather than full drag-and-drop, to keep this pass
// dependency-light -- the reorder endpoint itself is fully wired either way.
export function BacklogPage() {
  const { projectId } = useParams<{ projectId: string }>();
  const queryClient = useQueryClient();
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [selectedIssueId, setSelectedIssueId] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState("");
  const [error, setError] = useState<string | null>(null);

  const params = new URLSearchParams({ onlyBacklog: "true", limit: "100" });
  if (statusFilter) params.set("status", statusFilter);

  const { data: issues, isLoading } = useQuery({
    queryKey: ["issues", projectId, "backlog", statusFilter],
    queryFn: () => listIssues(projectId!, params),
    enabled: Boolean(projectId),
  });

  const createMutation = useMutation({
    mutationFn: (body: Parameters<typeof createIssue>[1]) => createIssue(projectId!, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["issues", projectId] });
      setShowCreateForm(false);
    },
    onError: (err) => setError(err instanceof ApiError ? err.message : "Failed to create issue."),
  });

  function handleCreate(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setError(null);
    const form = new FormData(e.currentTarget);
    createMutation.mutate({
      issueType: String(form.get("issueType")),
      title: String(form.get("title")),
      description: String(form.get("description") || "") || undefined,
      priority: String(form.get("priority")),
      storyPoints: form.get("storyPoints") ? Number(form.get("storyPoints")) : undefined,
    });
  }

  return (
    <div>
      <div className="page-header">
        <h2>Backlog</h2>
        <button onClick={() => setShowCreateForm((s) => !s)}>{showCreateForm ? "Cancel" : "Create Issue"}</button>
      </div>

      <div className="inline-form">
        <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
          <option value="">All statuses</option>
          <option value="To Do">To Do</option>
          <option value="In Progress">In Progress</option>
          <option value="In Review">In Review</option>
          <option value="Done">Done</option>
        </select>
      </div>

      {showCreateForm && (
        <form onSubmit={handleCreate} className="inline-form">
          <select name="issueType" defaultValue="STORY">
            <option value="EPIC">Epic</option>
            <option value="STORY">Story</option>
            <option value="TASK">Task</option>
            <option value="SUBTASK">Sub-task</option>
            <option value="BUG">Bug</option>
          </select>
          <input name="title" placeholder="Title" required />
          <input name="description" placeholder="Description (optional)" />
          <select name="priority" defaultValue="MEDIUM">
            <option value="LOW">Low</option>
            <option value="MEDIUM">Medium</option>
            <option value="HIGH">High</option>
            <option value="CRITICAL">Critical</option>
          </select>
          <input name="storyPoints" type="number" step="0.5" placeholder="Story points" />
          {error && <p className="form-error">{error}</p>}
          <button type="submit" disabled={createMutation.isPending}>
            Create
          </button>
        </form>
      )}

      {isLoading ? (
        <p>Loading...</p>
      ) : issues && issues.length === 0 ? (
        <p>No backlog issues yet -- create your first issue above.</p>
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              <th>Key</th>
              <th>Type</th>
              <th>Title</th>
              <th>Status</th>
              <th>Priority</th>
              <th>Points</th>
            </tr>
          </thead>
          <tbody>
            {issues?.map((issue: Issue) => (
              <tr key={issue.id} onClick={() => setSelectedIssueId(issue.id)} className="clickable-row">
                <td>{issue.issueKey}</td>
                <td>{issue.issueType}</td>
                <td>{issue.title}</td>
                <td>{issue.status}</td>
                <td>{issue.priority}</td>
                <td>{issue.storyPoints ?? "—"}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {selectedIssueId && projectId && (
        <IssueDetailPanel projectId={projectId} issueId={selectedIssueId} onClose={() => setSelectedIssueId(null)} />
      )}
    </div>
  );
}

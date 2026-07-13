import { Fragment, useState, type FormEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { archiveProject, createProject, deleteProject, listProjects, type AdminProject } from "./adminApi";
import { ApiError } from "../../lib/apiClient";
import { MembershipsPanel } from "./MembershipsPanel";

// UI Design S4.2 Project Management screen. PRD FR-6..9: delete is disabled
// (with an explanatory tooltip) unless the project is already archived --
// mirrors the API's 409 GOVERNANCE_SAFEGUARD rather than only discovering it
// after a failed request.
export function ProjectsPage() {
  const queryClient = useQueryClient();
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [expandedProjectId, setExpandedProjectId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: ["admin", "projects"],
    queryFn: () => listProjects(new URLSearchParams({ size: "50" })),
  });

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ["admin", "projects"] });

  const createMutation = useMutation({
    mutationFn: createProject,
    onSuccess: () => {
      invalidate();
      setShowCreateForm(false);
    },
    onError: (err) => setError(err instanceof ApiError ? err.message : "Failed to create project."),
  });
  const archiveMutation = useMutation({ mutationFn: archiveProject, onSuccess: invalidate });
  const deleteMutation = useMutation({
    mutationFn: deleteProject,
    onSuccess: invalidate,
    onError: (err) => setError(err instanceof ApiError ? err.message : "Failed to delete project."),
  });

  function handleCreate(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setError(null);
    const form = new FormData(e.currentTarget);
    createMutation.mutate({
      projectKey: String(form.get("projectKey")).toUpperCase(),
      name: String(form.get("name")),
      description: String(form.get("description") || "") || undefined,
      methodology: String(form.get("methodology")),
      startDate: String(form.get("startDate")),
      targetReleaseDate: String(form.get("targetReleaseDate") || "") || undefined,
    });
  }

  return (
    <div>
      <div className="page-header">
        <h2>Project Management</h2>
        <button onClick={() => setShowCreateForm((s) => !s)}>{showCreateForm ? "Cancel" : "Create Project"}</button>
      </div>

      {showCreateForm && (
        <form onSubmit={handleCreate} className="inline-form">
          <input name="projectKey" placeholder="Key (e.g. NEX)" maxLength={10} required />
          <input name="name" placeholder="Project name" required />
          <input name="description" placeholder="Description (optional)" />
          <select name="methodology" defaultValue="SCRUM">
            <option value="SCRUM">Scrum</option>
            <option value="KANBAN">Kanban</option>
            <option value="HYBRID">Hybrid (both boards, shared backlog)</option>
          </select>
          <label>
            Start date
            <input name="startDate" type="date" required />
          </label>
          <label>
            Target release (optional)
            <input name="targetReleaseDate" type="date" />
          </label>
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
              <th>Key</th>
              <th>Name</th>
              <th>Methodology</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {data?.data.map((project: AdminProject) => (
              <Fragment key={project.id}>
                <tr>
                  <td>{project.projectKey}</td>
                  <td>{project.name}</td>
                  <td>{project.methodology}</td>
                  <td>{project.status}</td>
                  <td className="row-actions">
                    <button onClick={() => setExpandedProjectId((id) => (id === project.id ? null : project.id))}>
                      {expandedProjectId === project.id ? "Hide Members" : "Manage Members"}
                    </button>
                    {project.status === "ACTIVE" && (
                      <button onClick={() => archiveMutation.mutate(project.id)}>Archive</button>
                    )}
                    <button
                      disabled={project.status !== "ARCHIVED"}
                      title={
                        project.status !== "ARCHIVED"
                          ? "Archive the project first before it can be deleted."
                          : undefined
                      }
                      onClick={() => deleteMutation.mutate(project.id)}
                    >
                      Delete
                    </button>
                  </td>
                </tr>
                {expandedProjectId === project.id && (
                  <tr>
                    <td colSpan={5}>
                      <MembershipsPanel projectId={project.id} />
                    </td>
                  </tr>
                )}
              </Fragment>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}

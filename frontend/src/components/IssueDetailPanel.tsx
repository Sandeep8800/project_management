import { useState, type FormEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  addComment,
  createLink,
  deleteIssue,
  getIssue,
  listComments,
  listLinks,
  transitionIssue,
  updateIssue,
} from "../features/backlog/backlogApi";
import { listWorkflowStatuses, listWorkflowTransitions } from "../features/sprintboard/sprintBoardApi";
import { ApiError } from "../lib/apiClient";
import { PermissionGate } from "./PermissionGate";

interface IssueDetailPanelProps {
  projectId: string;
  issueId: string;
  onClose: () => void;
}

/**
 * UI Design S4.6: shared Issue Detail component. The status control shows ONLY
 * the transitions legally reachable from the current status (client-side,
 * using the already-fetched workflow graph) -- the server's 422 remains the
 * actual authority if this client's cached graph is stale (UI Design S4.4).
 */
export function IssueDetailPanel({ projectId, issueId, onClose }: IssueDetailPanelProps) {
  const queryClient = useQueryClient();
  const [error, setError] = useState<string | null>(null);
  const [newComment, setNewComment] = useState("");
  const [linkTargetId, setLinkTargetId] = useState("");
  const [linkType, setLinkType] = useState("RELATES_TO");

  const { data: issue, isLoading } = useQuery({ queryKey: ["issue", issueId], queryFn: () => getIssue(issueId) });
  const { data: statuses } = useQuery({ queryKey: ["workflow", "statuses", projectId], queryFn: () => listWorkflowStatuses(projectId) });
  const { data: transitions } = useQuery({ queryKey: ["workflow", "transitions", projectId], queryFn: () => listWorkflowTransitions(projectId) });
  const { data: comments } = useQuery({ queryKey: ["comments", issueId], queryFn: () => listComments(issueId) });
  const { data: links } = useQuery({ queryKey: ["links", issueId], queryFn: () => listLinks(issueId) });

  const invalidateIssue = () => {
    queryClient.invalidateQueries({ queryKey: ["issue", issueId] });
    queryClient.invalidateQueries({ queryKey: ["issues"] });
    queryClient.invalidateQueries({ queryKey: ["board"] });
  };

  const transitionMutation = useMutation({
    mutationFn: (targetStatus: string) => transitionIssue(projectId, issueId, issue!.version, targetStatus),
    onSuccess: invalidateIssue,
    onError: (err) => setError(err instanceof ApiError ? err.message : "Transition failed."),
  });

  const editMutation = useMutation({
    mutationFn: (body: { title?: string; description?: string; priority?: string }) =>
      updateIssue(projectId, issueId, issue!.version, body),
    onSuccess: invalidateIssue,
    onError: (err) => {
      if (err instanceof ApiError && err.code === "CONCURRENT_MODIFICATION") {
        setError("This issue changed since you loaded it -- reload to see the latest version.");
      } else {
        setError(err instanceof ApiError ? err.message : "Update failed.");
      }
    },
  });

  const deleteMutation = useMutation({
    mutationFn: () => deleteIssue(projectId, issueId),
    onSuccess: () => {
      invalidateIssue();
      onClose();
    },
  });

  const commentMutation = useMutation({
    mutationFn: () => addComment(projectId, issueId, newComment),
    onSuccess: () => {
      setNewComment("");
      queryClient.invalidateQueries({ queryKey: ["comments", issueId] });
    },
  });

  const linkMutation = useMutation({
    mutationFn: () => createLink(projectId, issueId, linkTargetId, linkType),
    onSuccess: () => {
      setLinkTargetId("");
      queryClient.invalidateQueries({ queryKey: ["links", issueId] });
    },
  });

  function legalNextStatuses(): string[] {
    if (!issue || !statuses || !transitions) return [];
    const currentStatus = statuses.find((s) => s.name === issue.status);
    if (!currentStatus) return [];
    return transitions
        .filter((t) => t.fromStatusId === currentStatus.id)
        .map((t) => statuses.find((s) => s.id === t.toStatusId)?.name)
        .filter((n): n is string => Boolean(n));
  }

  function handleSaveEdit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setError(null);
    const form = new FormData(e.currentTarget);
    editMutation.mutate({
      title: String(form.get("title")),
      description: String(form.get("description") || ""),
      priority: String(form.get("priority")),
    });
  }

  return (
    <div className="slide-over-backdrop" onClick={onClose}>
      <div className="slide-over-panel" onClick={(e) => e.stopPropagation()}>
        <button className="slide-over-close" onClick={onClose}>
          Close
        </button>
        {isLoading || !issue ? (
          <p>Loading...</p>
        ) : (
          <>
            <h2>
              {issue.issueKey} <span className="type-badge">{issue.issueType}</span>
            </h2>

            <div className="status-control">
              <strong>Status:</strong> {issue.status}
              <PermissionGate>
                <div className="status-transitions">
                  {legalNextStatuses().map((status) => (
                    <button key={status} onClick={() => transitionMutation.mutate(status)} disabled={transitionMutation.isPending}>
                      &rarr; {status}
                    </button>
                  ))}
                </div>
              </PermissionGate>
            </div>

            <form onSubmit={handleSaveEdit} className="issue-edit-form">
              <label>
                Title
                <input name="title" defaultValue={issue.title} />
              </label>
              <label>
                Description
                <textarea name="description" defaultValue={issue.description} />
              </label>
              <label>
                Priority
                <input name="priority" defaultValue={issue.priority} />
              </label>
              {error && <p className="form-error">{error}</p>}
              <button type="submit" disabled={editMutation.isPending}>
                Save
              </button>
              <button type="button" onClick={() => deleteMutation.mutate()} className="danger">
                Delete Issue
              </button>
            </form>

            <section>
              <h3>Linked Issues</h3>
              <ul>
                {links?.map((l) => (
                  <li key={l.id}>
                    {l.linkType}: {l.sourceIssueId === issueId ? l.targetIssueId : l.sourceIssueId}
                  </li>
                ))}
              </ul>
              <div className="inline-form">
                <input placeholder="Target issue ID" value={linkTargetId} onChange={(e) => setLinkTargetId(e.target.value)} />
                <select value={linkType} onChange={(e) => setLinkType(e.target.value)}>
                  <option value="BLOCKS">Blocks</option>
                  <option value="IS_BLOCKED_BY">Is Blocked By</option>
                  <option value="RELATES_TO">Relates To</option>
                  <option value="DUPLICATES">Duplicates</option>
                </select>
                <button onClick={() => linkMutation.mutate()} disabled={!linkTargetId}>
                  Add Link
                </button>
              </div>
            </section>

            <section>
              <h3>Comments</h3>
              <ul className="comment-list">
                {comments?.data.map((c) => (
                  <li key={c.id}>
                    <div className="comment-meta">{new Date(c.createdAt).toLocaleString()}</div>
                    <div>{c.body}</div>
                  </li>
                ))}
              </ul>
              <div className="inline-form">
                <textarea value={newComment} onChange={(e) => setNewComment(e.target.value)} placeholder="Add a comment..." />
                <button onClick={() => commentMutation.mutate()} disabled={!newComment.trim()}>
                  Comment
                </button>
              </div>
            </section>
          </>
        )}
      </div>
    </div>
  );
}

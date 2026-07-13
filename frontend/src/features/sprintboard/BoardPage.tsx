import { useState, type DragEvent } from "react";
import { useParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { getBoard, type BoardColumn } from "./sprintBoardApi";
import { transitionIssue, type Issue } from "../backlog/backlogApi";
import { ApiError } from "../../lib/apiClient";
import { IssueDetailPanel } from "../../components/IssueDetailPanel";

/**
 * UI Design S4.4: Scrum/Kanban board with native HTML5 drag-and-drop between
 * columns. A drop triggers the same transitions endpoint the status dropdown
 * uses; illegal drops still get the server's 422 (WorkflowTransitionValidator)
 * as the real authority -- this client doesn't pre-validate the drop target
 * against the workflow graph the way IssueDetailPanel's dropdown does, so a
 * rejected drop surfaces as an error banner rather than being blocked before
 * the request, a known simplification versus UI Design S4.4's full spec.
 */
export function BoardPage() {
  const { projectId } = useParams<{ projectId: string }>();
  const queryClient = useQueryClient();
  const [boardType, setBoardType] = useState<"SCRUM" | "KANBAN">("KANBAN");
  const [selectedIssueId, setSelectedIssueId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const { data: columns, isLoading } = useQuery({
    queryKey: ["board", projectId, boardType],
    queryFn: () => getBoard(projectId!, boardType),
    enabled: Boolean(projectId),
  });

  const transitionMutation = useMutation({
    mutationFn: ({ issue, targetStatus }: { issue: Issue; targetStatus: string }) =>
      transitionIssue(projectId!, issue.id, issue.version, targetStatus),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["board", projectId] }),
    onError: (err) => setError(err instanceof ApiError ? err.message : "Could not move the issue."),
  });

  function handleDrop(e: DragEvent, column: BoardColumn) {
    e.preventDefault();
    setError(null);
    const issueId = e.dataTransfer.getData("text/issue-id");
    const issue = columns?.flatMap((c) => c.issues).find((i) => i.id === issueId);
    const targetStatus = column.statusNames[0];
    if (issue && targetStatus && issue.status !== targetStatus) {
      transitionMutation.mutate({ issue, targetStatus });
    }
  }

  return (
    <div>
      <div className="page-header">
        <h2>Board</h2>
        <div className="board-type-tabs">
          <button className={boardType === "SCRUM" ? "active" : ""} onClick={() => setBoardType("SCRUM")}>
            Scrum
          </button>
          <button className={boardType === "KANBAN" ? "active" : ""} onClick={() => setBoardType("KANBAN")}>
            Kanban
          </button>
        </div>
      </div>
      {error && <p className="form-error">{error}</p>}

      {isLoading ? (
        <p>Loading...</p>
      ) : boardType === "SCRUM" && columns?.every((c) => c.issues.length === 0) ? (
        <p>No active sprint, or the active sprint has no issues yet.</p>
      ) : (
        <div className="board-columns">
          {columns?.map((column) => (
            <div key={column.columnId} className="board-column" onDragOver={(e) => e.preventDefault()} onDrop={(e) => handleDrop(e, column)}>
              <div className="board-column-header">
                {column.name}
                {column.wipLimit != null && (
                  <span className={column.issues.length >= column.wipLimit ? "wip-limit-warning" : "wip-limit"}>
                    {column.issues.length} / {column.wipLimit}
                  </span>
                )}
              </div>
              {column.issues.map((issue) => (
                <div
                  key={issue.id}
                  className="issue-card"
                  draggable
                  onDragStart={(e) => e.dataTransfer.setData("text/issue-id", issue.id)}
                  onClick={() => setSelectedIssueId(issue.id)}
                >
                  <div className="issue-card-key">{issue.issueKey}</div>
                  <div>{issue.title}</div>
                </div>
              ))}
            </div>
          ))}
        </div>
      )}

      {selectedIssueId && projectId && (
        <IssueDetailPanel projectId={projectId} issueId={selectedIssueId} onClose={() => setSelectedIssueId(null)} />
      )}
    </div>
  );
}

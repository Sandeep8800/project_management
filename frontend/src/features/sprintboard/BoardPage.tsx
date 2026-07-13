import { useState, type DragEvent } from "react";
import { useParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { getBoard, listWorkflowStatuses, listWorkflowTransitions, type BoardColumn } from "./sprintBoardApi";
import { transitionIssue, type Issue } from "../backlog/backlogApi";
import { ApiError } from "../../lib/apiClient";
import { IssueDetailPanel } from "../../components/IssueDetailPanel";

/**
 * UI Design S4.4: Scrum/Kanban board with native HTML5 drag-and-drop between
 * columns. Illegal drops are blocked client-side using the already-fetched
 * workflow graph (same computation as IssueDetailPanel's status dropdown) --
 * the server's 422 (WorkflowTransitionValidator) remains the actual authority
 * if this client's cached graph is stale, per UI Design Principle 1.
 */
export function BoardPage() {
  const { projectId } = useParams<{ projectId: string }>();
  const queryClient = useQueryClient();
  const [boardType, setBoardType] = useState<"SCRUM" | "KANBAN">("KANBAN");
  const [selectedIssueId, setSelectedIssueId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [draggedIssue, setDraggedIssue] = useState<Issue | null>(null);

  const { data: columns, isLoading } = useQuery({
    queryKey: ["board", projectId, boardType],
    queryFn: () => getBoard(projectId!, boardType),
    enabled: Boolean(projectId),
  });
  const { data: statuses } = useQuery({ queryKey: ["workflow", "statuses", projectId], queryFn: () => listWorkflowStatuses(projectId!), enabled: Boolean(projectId) });
  const { data: transitions } = useQuery({ queryKey: ["workflow", "transitions", projectId], queryFn: () => listWorkflowTransitions(projectId!), enabled: Boolean(projectId) });

  const transitionMutation = useMutation({
    mutationFn: ({ issue, targetStatus }: { issue: Issue; targetStatus: string }) =>
      transitionIssue(projectId!, issue.id, issue.version, targetStatus),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["board", projectId] }),
    onError: (err) => setError(err instanceof ApiError ? err.message : "Could not move the issue."),
  });

  function legalTargetStatuses(fromStatus: string): Set<string> {
    if (!statuses || !transitions) return new Set();
    const fromStatusId = statuses.find((s) => s.name === fromStatus)?.id;
    if (!fromStatusId) return new Set();
    return new Set(
      transitions
        .filter((t) => t.fromStatusId === fromStatusId)
        .map((t) => statuses.find((s) => s.id === t.toStatusId)?.name)
        .filter((n): n is string => Boolean(n))
    );
  }

  function isLegalDropColumn(column: BoardColumn): boolean {
    if (!draggedIssue) return false;
    const legal = legalTargetStatuses(draggedIssue.status);
    return column.statusNames.some((s) => legal.has(s));
  }

  function handleDrop(e: DragEvent, column: BoardColumn) {
    e.preventDefault();
    setError(null);
    const issueId = e.dataTransfer.getData("text/issue-id");
    const issue = columns?.flatMap((c) => c.issues).find((i) => i.id === issueId);
    setDraggedIssue(null);
    if (!issue) return;

    const legal = legalTargetStatuses(issue.status);
    const targetStatus = column.statusNames.find((s) => legal.has(s));
    if (!targetStatus) {
      if (!column.statusNames.includes(issue.status)) {
        setError(`Cannot move ${issue.issueKey} from "${issue.status}" to "${column.name}" -- that transition isn't allowed by this project's workflow.`);
      }
      return;
    }
    if (issue.status !== targetStatus) {
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
          {columns?.map((column) => {
            const isDropTarget = draggedIssue != null;
            const isLegal = isDropTarget && isLegalDropColumn(column);
            return (
              <div
                key={column.columnId}
                className={
                  "board-column" +
                  (isDropTarget ? (isLegal ? " drop-target-legal" : " drop-target-illegal") : "")
                }
                onDragOver={(e) => e.preventDefault()}
                onDrop={(e) => handleDrop(e, column)}
              >
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
                    onDragStart={(e) => {
                      e.dataTransfer.setData("text/issue-id", issue.id);
                      setDraggedIssue(issue);
                    }}
                    onDragEnd={() => setDraggedIssue(null)}
                    onClick={() => setSelectedIssueId(issue.id)}
                  >
                    <div className="issue-card-key">{issue.issueKey}</div>
                    <div>{issue.title}</div>
                  </div>
                ))}
              </div>
            );
          })}
        </div>
      )}

      {selectedIssueId && projectId && (
        <IssueDetailPanel projectId={projectId} issueId={selectedIssueId} onClose={() => setSelectedIssueId(null)} />
      )}
    </div>
  );
}

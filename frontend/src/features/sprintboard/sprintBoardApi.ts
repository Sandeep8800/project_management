import { apiRequest } from "../../lib/apiClient";
import type { Issue } from "../backlog/backlogApi";

export interface Sprint {
  id: string;
  projectId: string;
  name: string;
  goal?: string;
  startDate?: string;
  endDate?: string;
  status: "PLANNED" | "ACTIVE" | "COMPLETED";
  version: number;
}

export interface BoardColumn {
  columnId: string;
  name: string;
  displayOrder: number;
  wipLimit?: number;
  statusNames: string[];
  issues: Issue[];
}

export interface WorkflowStatus {
  id: string;
  name: string;
  displayOrder: number;
  isInitial: boolean;
  isTerminal: boolean;
}

export interface WorkflowTransition {
  id: string;
  fromStatusId: string;
  toStatusId: string;
}

// API Design S7 (sprint & board)
export const listSprints = (projectId: string, status?: string) =>
  apiRequest<Sprint[]>(`/projects/${projectId}/sprints${status ? `?status=${status}` : ""}`);

export const createSprint = (projectId: string, name: string, goal?: string) =>
  apiRequest<Sprint>(`/projects/${projectId}/sprints`, { method: "POST", body: { name, goal } });

export const startSprint = (projectId: string, sprintId: string, version: number, startDate: string, endDate: string) =>
  apiRequest<Sprint>(`/projects/${projectId}/sprints/${sprintId}/start`, {
    method: "POST",
    body: { startDate, endDate },
    ifMatch: String(version),
  });

export const completeSprint = (
  projectId: string,
  sprintId: string,
  version: number,
  rolloverDecision: "MOVE_TO_BACKLOG" | "MOVE_TO_NEXT_SPRINT",
  targetSprintId?: string
) =>
  apiRequest<Sprint>(`/projects/${projectId}/sprints/${sprintId}/complete`, {
    method: "POST",
    body: { rolloverDecision, targetSprintId },
    ifMatch: String(version),
  });

export const getBoard = (projectId: string, boardType: "SCRUM" | "KANBAN") =>
  apiRequest<BoardColumn[]>(`/projects/${projectId}/boards/${boardType}`);

export const configureColumn = (projectId: string, boardType: string, columnId: string, wipLimit?: number) =>
  apiRequest<void>(`/projects/${projectId}/boards/${boardType}/columns/${columnId}`, { method: "PATCH", body: { wipLimit } });

export const listWorkflowStatuses = (projectId: string) =>
  apiRequest<WorkflowStatus[]>(`/projects/${projectId}/workflow/statuses`);

export const listWorkflowTransitions = (projectId: string) =>
  apiRequest<WorkflowTransition[]>(`/projects/${projectId}/workflow/transitions`);

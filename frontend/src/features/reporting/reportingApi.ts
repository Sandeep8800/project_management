import { apiRequest } from "../../lib/apiClient";

export interface BurndownPoint {
  date: string;
  remainingPoints: string;
  remainingIssueCount: number;
}

export interface VelocityPoint {
  sprintId: string;
  committedPoints: string;
  completedPoints: string;
}

export interface CfdPoint {
  date: string;
  statusName: string;
  issueCount: number;
}

export interface SprintSummary {
  sprintId: string;
  plannedPoints: string;
  completedPoints: string;
  scopeAddedPoints: string;
  scopeRemovedPoints: string;
  carryOverIssueCount: number;
}

// API Design S8 (read-only reporting)
export const getBurndown = (projectId: string, sprintId: string) =>
  apiRequest<BurndownPoint[]>(`/projects/${projectId}/sprints/${sprintId}/reports/burndown`);

export const getVelocity = (projectId: string) => apiRequest<VelocityPoint[]>(`/projects/${projectId}/reports/velocity`);

export const getCfd = (projectId: string, boardType: string, from: string, to: string) =>
  apiRequest<CfdPoint[]>(`/projects/${projectId}/boards/${boardType}/reports/cfd?from=${from}&to=${to}`);

export const getSprintSummary = (projectId: string, sprintId: string) =>
  apiRequest<SprintSummary>(`/projects/${projectId}/sprints/${sprintId}/reports/summary`);

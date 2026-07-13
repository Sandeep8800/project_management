import { apiRequest } from "../../lib/apiClient";

export interface Issue {
  id: string;
  projectId: string;
  issueKey: string;
  issueType: "EPIC" | "STORY" | "TASK" | "SUBTASK" | "BUG";
  parentIssueId?: string;
  title: string;
  description?: string;
  status: string;
  assigneeId?: string;
  reporterId: string;
  priority: string;
  storyPoints?: string;
  sprintId?: string;
  backlogRank: string;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface Comment {
  id: string;
  issueId: string;
  authorId: string;
  body: string;
  createdAt: string;
}

export interface IssueLink {
  id: string;
  sourceIssueId: string;
  targetIssueId: string;
  linkType: string;
}

export interface Label {
  id: string;
  projectId: string;
  name: string;
}

// API Design S6 (backlog & issues)
export const listIssues = (projectId: string, params: URLSearchParams) =>
  apiRequest<Issue[]>(`/projects/${projectId}/issues?${params}`);

export const getIssue = (issueId: string) => apiRequest<Issue>(`/issues/${issueId}`);

export const createIssue = (
  projectId: string,
  body: {
    issueType: string;
    parentIssueId?: string;
    title: string;
    description?: string;
    priority: string;
    storyPoints?: number;
    assigneeId?: string;
  }
) => apiRequest<Issue>(`/projects/${projectId}/issues`, { method: "POST", body });

export const updateIssue = (
  projectId: string,
  issueId: string,
  version: number,
  body: { title?: string; description?: string; priority?: string; storyPoints?: number; assigneeId?: string }
) => apiRequest<Issue>(`/projects/${projectId}/issues/${issueId}`, { method: "PATCH", body, ifMatch: String(version) });

export const deleteIssue = (projectId: string, issueId: string) =>
  apiRequest<void>(`/projects/${projectId}/issues/${issueId}`, { method: "DELETE" });

export const transitionIssue = (projectId: string, issueId: string, version: number, targetStatus: string) =>
  apiRequest<Issue>(`/projects/${projectId}/issues/${issueId}/transitions`, {
    method: "POST",
    body: { targetStatus },
    ifMatch: String(version),
  });

export const reorderIssue = (projectId: string, issueId: string, afterIssueId?: string, beforeIssueId?: string) =>
  apiRequest<Issue>(`/projects/${projectId}/backlog/reorder`, { method: "PATCH", body: { issueId, afterIssueId, beforeIssueId } });

export const moveIssueToSprint = (projectId: string, issueId: string, version: number, sprintId: string | null) =>
  apiRequest<Issue>(`/projects/${projectId}/issues/${issueId}/sprint`, {
    method: "PATCH",
    body: { sprintId },
    ifMatch: String(version),
  });

export const listComments = (issueId: string) => apiRequest<{ data: Comment[] }>(`/issues/${issueId}/comments`);

export const addComment = (projectId: string, issueId: string, body: string, mentionedUserIds?: string[]) =>
  apiRequest<Comment>(`/projects/${projectId}/issues/${issueId}/comments`, { method: "POST", body: { body, mentionedUserIds } });

export const listLinks = (issueId: string) => apiRequest<IssueLink[]>(`/issues/${issueId}/links`);

export const createLink = (projectId: string, issueId: string, targetIssueId: string, linkType: string) =>
  apiRequest<IssueLink>(`/projects/${projectId}/issues/${issueId}/links`, { method: "POST", body: { targetIssueId, linkType } });

export const deleteLink = (projectId: string, linkId: string) =>
  apiRequest<void>(`/projects/${projectId}/issue-links/${linkId}`, { method: "DELETE" });

export const listLabels = (projectId: string) => apiRequest<Label[]>(`/projects/${projectId}/labels`);

export const createLabel = (projectId: string, name: string) =>
  apiRequest<Label>(`/projects/${projectId}/labels`, { method: "POST", body: { name } });

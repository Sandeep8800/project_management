import { apiRequest } from "../../lib/apiClient";

export interface Notification {
  id: string;
  eventType: string;
  payload: Record<string, unknown>;
  readAt?: string;
  createdAt: string;
}

interface PageResponse<T> {
  data: T[];
  pagination: { hasMore: boolean };
}

// API Design S9
export const listNotifications = (unreadOnly: boolean) =>
  apiRequest<PageResponse<Notification>>(`/notifications?unreadOnly=${unreadOnly}&size=50`);

export const markRead = (id: string) => apiRequest<void>(`/notifications/${id}/read`, { method: "POST" });

export const markAllRead = () => apiRequest<void>(`/notifications/read-all`, { method: "POST" });

export const getPreferences = () => apiRequest<{ inAppEnabled: boolean; emailEnabled: boolean }>("/notifications/preferences");

export const updatePreferences = (emailEnabled: boolean) =>
  apiRequest<{ inAppEnabled: boolean; emailEnabled: boolean }>("/notifications/preferences", {
    method: "PATCH",
    body: { emailEnabled },
  });

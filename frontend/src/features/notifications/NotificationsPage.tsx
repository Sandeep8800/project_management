import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { getPreferences, listNotifications, markAllRead, markRead, updatePreferences } from "./notificationsApi";

/** UI Design S4.8: full notifications page + preferences (single email toggle -- in-app is always on, HLD S8.1). */
export function NotificationsPage() {
  const queryClient = useQueryClient();

  const { data } = useQuery({ queryKey: ["notifications", "all"], queryFn: () => listNotifications(false) });
  const { data: preferences } = useQuery({ queryKey: ["notifications", "preferences"], queryFn: getPreferences });

  const markReadMutation = useMutation({
    mutationFn: markRead,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["notifications"] }),
  });
  const markAllReadMutation = useMutation({
    mutationFn: markAllRead,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["notifications"] }),
  });
  const updatePreferencesMutation = useMutation({
    mutationFn: updatePreferences,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["notifications", "preferences"] }),
  });

  return (
    <div>
      <div className="page-header">
        <h2>Notifications</h2>
        <button onClick={() => markAllReadMutation.mutate()}>Mark all read</button>
      </div>

      <label className="preference-toggle">
        <input
          type="checkbox"
          checked={preferences?.emailEnabled ?? true}
          onChange={(e) => updatePreferencesMutation.mutate(e.target.checked)}
        />
        Email notifications (in-app notifications are always on)
      </label>

      <table className="data-table">
        <thead>
          <tr>
            <th>Event</th>
            <th>Received</th>
            <th>Status</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {data?.data.map((n) => (
            <tr key={n.id}>
              <td>{n.eventType}</td>
              <td>{new Date(n.createdAt).toLocaleString()}</td>
              <td>{n.readAt ? "Read" : "Unread"}</td>
              <td>{!n.readAt && <button onClick={() => markReadMutation.mutate(n.id)}>Mark read</button>}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

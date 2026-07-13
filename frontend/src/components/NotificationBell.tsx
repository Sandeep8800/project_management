import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { listNotifications, markAllRead, markRead } from "../features/notifications/notificationsApi";

/** UI Design S4.8: bell dropdown in the global nav. Real-time push (HLD S8.4) is not wired into this polling-based query in this pass -- it refreshes on open/mutation, not via the WebSocket connection. */
export function NotificationBell() {
  const [open, setOpen] = useState(false);
  const queryClient = useQueryClient();

  const { data } = useQuery({ queryKey: ["notifications", "unread"], queryFn: () => listNotifications(true) });

  const markReadMutation = useMutation({
    mutationFn: markRead,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["notifications"] }),
  });
  const markAllReadMutation = useMutation({
    mutationFn: markAllRead,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["notifications"] }),
  });

  const unreadCount = data?.data.length ?? 0;

  return (
    <div className="notification-bell">
      <button onClick={() => setOpen((o) => !o)}>
        Notifications {unreadCount > 0 && <span className="unread-badge">{unreadCount}</span>}
      </button>
      {open && (
        <div className="notification-dropdown">
          {unreadCount === 0 ? (
            <p>No unread notifications.</p>
          ) : (
            <>
              <button onClick={() => markAllReadMutation.mutate()}>Mark all read</button>
              <ul>
                {data?.data.map((n) => (
                  <li key={n.id}>
                    <span>{n.eventType}</span>
                    <button onClick={() => markReadMutation.mutate(n.id)}>Mark read</button>
                  </li>
                ))}
              </ul>
            </>
          )}
        </div>
      )}
    </div>
  );
}

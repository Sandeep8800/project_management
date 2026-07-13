/**
 * Notifications module (HLD S4/S8.1, LLD S8, Database Design S9, API Design S9),
 * plus the Background Job worker (HLD S8.5, LLD S10) and Real-Time push (HLD
 * S8.4) that share its event-driven shape.
 *
 * SCAFFOLDED, NOT IMPLEMENTED in this pass. Owns in-app + email notification
 * dispatch via the transactional outbox pattern, and (as a separate consumer of
 * the same domain events) WebSocket/SSE board and notification push.
 *
 * Follow-up work: NotificationDispatchService + channel strategy (InAppChannel/
 * EmailChannel), the outbox-polling background worker, WebSocket/STOMP
 * configuration with the Redis-backed subscription registry for horizontal
 * scaling (HLD S8.4), and the background_jobs table's worker loop shared with
 * sprint-rollover and projection-rebuild job types.
 */
package com.nexuspms.notifications;

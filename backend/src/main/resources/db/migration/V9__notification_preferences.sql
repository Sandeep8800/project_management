-- HLD S8.1 / UI Design S4.8 require a per-user email-notification toggle
-- ("in-app always on, email opt-in") but Database Design S9 didn't add a
-- concrete column for it -- found while wiring the Notifications module.
ALTER TABLE users ADD COLUMN email_notifications_enabled boolean NOT NULL DEFAULT true;

/**
 * Notifications module (HLD S4/S8.1, LLD S8, Database Design S9, API Design S9),
 * plus the Background Job worker (HLD S8.5, LLD S10) and Real-Time push (HLD
 * S8.4) that share its event-driven shape.
 *
 * Owns in-app + email notification dispatch via the transactional outbox
 * pattern. In-app delivery writes the read-model directly; email delivery
 * (EmailDeliveryChannel) sends real SMTP via JavaMailSender when
 * nexus.mail.enabled=true, logging instead when it's not (the default, since
 * no SMTP server is provisioned in this environment).
 */
package com.nexuspms.notifications;

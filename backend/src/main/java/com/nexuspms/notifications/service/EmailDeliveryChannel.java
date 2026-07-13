package com.nexuspms.notifications.service;

import com.nexuspms.notifications.domain.NotificationChannel;
import com.nexuspms.notifications.domain.NotificationOutbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * STUB: no SMTP/email provider is wired in this pass (HLD S3 lists it as an
 * external system, not provisioned here). Logs what would have been sent so
 * the outbox->dispatch->channel flow is exercised end to end; swapping in a
 * real provider (JavaMailSender, SES, etc.) only touches this class.
 */
@Component
public class EmailDeliveryChannel implements DeliveryChannel {

    private static final Logger log = LoggerFactory.getLogger(EmailDeliveryChannel.class);

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public void deliver(NotificationOutbox outboxEntry) {
        log.info("[STUB EMAIL] to recipient {} -- event {} -- payload {}",
                outboxEntry.getRecipientId(), outboxEntry.getEventType(), outboxEntry.getPayload());
    }
}

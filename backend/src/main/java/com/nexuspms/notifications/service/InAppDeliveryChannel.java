package com.nexuspms.notifications.service;

import com.nexuspms.notifications.domain.Notification;
import com.nexuspms.notifications.domain.NotificationChannel;
import com.nexuspms.notifications.domain.NotificationOutbox;
import com.nexuspms.notifications.repository.NotificationRepository;
import org.springframework.stereotype.Component;

/** Always on (HLD S8.1) -- writes the row the bell/inbox UI reads. */
@Component
public class InAppDeliveryChannel implements DeliveryChannel {

    private final NotificationRepository notificationRepository;

    public InAppDeliveryChannel(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.IN_APP;
    }

    @Override
    public void deliver(NotificationOutbox outboxEntry) {
        notificationRepository.save(new Notification(outboxEntry.getRecipientId(), outboxEntry.getEventType(), outboxEntry.getPayload()));
    }
}

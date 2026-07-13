package com.nexuspms.notifications.service;

import com.nexuspms.notifications.domain.NotificationChannel;
import com.nexuspms.notifications.domain.NotificationOutbox;

/** LLD S8: one implementation per NotificationChannel value -- adding a channel later means adding an implementation, not changing NotificationDispatchJobHandler. */
public interface DeliveryChannel {

    NotificationChannel channel();

    void deliver(NotificationOutbox outboxEntry);
}

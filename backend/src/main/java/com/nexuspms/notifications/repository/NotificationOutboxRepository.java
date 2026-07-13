package com.nexuspms.notifications.repository;

import com.nexuspms.notifications.domain.NotificationOutbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, UUID> {
}

package com.nexuspms.notifications.service;

import com.nexuspms.common.exception.ResourceNotFoundException;
import com.nexuspms.notifications.domain.Notification;
import com.nexuspms.notifications.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** API Design S9: notifications are always scoped to the authenticated caller as recipient -- no project-level permission applies. */
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public Page<Notification> list(UUID recipientId, boolean unreadOnly, Pageable pageable) {
        return notificationRepository.findForRecipient(recipientId, unreadOnly, pageable);
    }

    @Transactional
    public void markRead(UUID recipientId, UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .filter(n -> n.getRecipientId().equals(recipientId))
                .orElseThrow(() -> new ResourceNotFoundException("Notification " + notificationId + " not found."));
        notification.markRead();
    }

    @Transactional
    public void markAllRead(UUID recipientId) {
        List<Notification> unread = notificationRepository.findForRecipient(recipientId, true, org.springframework.data.domain.Pageable.unpaged()).getContent();
        unread.forEach(Notification::markRead);
    }
}

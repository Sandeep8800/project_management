package com.nexuspms.notifications.repository;

import com.nexuspms.notifications.domain.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    @Query("""
            select n from Notification n
            where n.recipientId = :recipientId
              and (:unreadOnly = false or n.readAt is null)
            order by n.createdAt desc
            """)
    Page<Notification> findForRecipient(@Param("recipientId") UUID recipientId, @Param("unreadOnly") boolean unreadOnly, Pageable pageable);

    long countByRecipientIdAndReadAtIsNull(UUID recipientId);
}

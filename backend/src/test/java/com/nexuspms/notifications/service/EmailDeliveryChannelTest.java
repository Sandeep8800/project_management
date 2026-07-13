package com.nexuspms.notifications.service;

import com.nexuspms.identity.domain.User;
import com.nexuspms.identity.repository.UserRepository;
import com.nexuspms.notifications.domain.NotificationChannel;
import com.nexuspms.notifications.domain.NotificationOutbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pins the enabled/disabled branching: nexus.mail.enabled=false must never touch JavaMailSender, regardless of what else is configured. */
@ExtendWith(MockitoExtension.class)
class EmailDeliveryChannelTest {

    @Mock
    JavaMailSender mailSender;
    @Mock
    UserRepository userRepository;

    @Test
    void disabled_neverCallsMailSender() {
        EmailDeliveryChannel channel = new EmailDeliveryChannel(mailSender, userRepository, false, "no-reply@nexus-pms.local");
        NotificationOutbox outboxEntry = new NotificationOutbox(UUID.randomUUID(), "ISSUE_ASSIGNED", Map.of("issueId", "x"), NotificationChannel.EMAIL);

        channel.deliver(outboxEntry);

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void enabled_sendsToRecipientsEmail() {
        EmailDeliveryChannel channel = new EmailDeliveryChannel(mailSender, userRepository, true, "no-reply@nexus-pms.local");
        UUID recipientId = UUID.randomUUID();
        User recipient = new User("Dev", "dev@example.com", null, null, "DEVELOPER");
        NotificationOutbox outboxEntry = new NotificationOutbox(recipientId, "ISSUE_ASSIGNED", Map.of("issueId", "x"), NotificationChannel.EMAIL);
        when(userRepository.findById(recipientId)).thenReturn(Optional.of(recipient));

        channel.deliver(outboxEntry);

        verify(mailSender).send(any(SimpleMailMessage.class));
    }
}

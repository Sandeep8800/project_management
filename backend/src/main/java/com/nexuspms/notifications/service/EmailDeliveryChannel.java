package com.nexuspms.notifications.service;

import com.nexuspms.identity.domain.User;
import com.nexuspms.identity.repository.UserRepository;
import com.nexuspms.notifications.domain.NotificationChannel;
import com.nexuspms.notifications.domain.NotificationOutbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * HLD S3: SMTP is an external system. When nexus.mail.enabled=true (and
 * spring.mail.host is configured), this sends a real email via JavaMailSender.
 * Otherwise it logs what would have been sent and returns -- no environment in
 * this pass has a real SMTP server provisioned, so log-only is the honest
 * default rather than failing loudly on every notification.
 */
@Component
public class EmailDeliveryChannel implements DeliveryChannel {

    private static final Logger log = LoggerFactory.getLogger(EmailDeliveryChannel.class);

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;
    private final boolean mailEnabled;
    private final String fromAddress;

    public EmailDeliveryChannel(JavaMailSender mailSender, UserRepository userRepository,
                                 @Value("${nexus.mail.enabled}") boolean mailEnabled,
                                 @Value("${nexus.mail.from}") String fromAddress) {
        this.mailSender = mailSender;
        this.userRepository = userRepository;
        this.mailEnabled = mailEnabled;
        this.fromAddress = fromAddress;
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public void deliver(NotificationOutbox outboxEntry) {
        if (!mailEnabled) {
            log.info("[STUB EMAIL, nexus.mail.enabled=false] to recipient {} -- event {} -- payload {}",
                    outboxEntry.getRecipientId(), outboxEntry.getEventType(), outboxEntry.getPayload());
            return;
        }

        User recipient = userRepository.findById(outboxEntry.getRecipientId())
                .orElseThrow(() -> new IllegalStateException("Notification recipient " + outboxEntry.getRecipientId() + " no longer exists."));

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(recipient.getEmail());
        message.setSubject(subjectFor(outboxEntry.getEventType()));
        message.setText(bodyFor(outboxEntry.getEventType(), outboxEntry.getPayload()));
        mailSender.send(message);
    }

    private String subjectFor(String eventType) {
        return "Nexus PMS: " + eventType.replace('_', ' ').toLowerCase();
    }

    private String bodyFor(String eventType, Map<String, Object> payload) {
        StringBuilder body = new StringBuilder(eventType.replace('_', ' ')).append("\n\n");
        payload.forEach((key, value) -> body.append(key).append(": ").append(value).append('\n'));
        body.append("\nView in Nexus PMS to see details and take action.");
        return body.toString();
    }
}

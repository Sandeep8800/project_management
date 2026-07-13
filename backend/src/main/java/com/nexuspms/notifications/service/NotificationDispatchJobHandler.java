package com.nexuspms.notifications.service;

import com.nexuspms.common.job.JobHandler;
import com.nexuspms.notifications.domain.NotificationOutbox;
import com.nexuspms.notifications.domain.OutboxStatus;
import com.nexuspms.notifications.repository.NotificationOutboxRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * LLD S8/S10: the background-job side of the outbox pattern. Failed email
 * sends retry with backoff up to a bounded attempt count, then land in
 * DEAD_LETTERED -- visible to Admin so silently-failed notifications are
 * discoverable (LLD S8), not built as an admin UI in this pass, but the status
 * is there to query.
 */
@Component
public class NotificationDispatchJobHandler implements JobHandler {

    private static final int MAX_OUTBOX_ATTEMPTS = 5;

    private final NotificationOutboxRepository notificationOutboxRepository;
    private final Map<com.nexuspms.notifications.domain.NotificationChannel, DeliveryChannel> channelsByType;

    public NotificationDispatchJobHandler(NotificationOutboxRepository notificationOutboxRepository, List<DeliveryChannel> channels) {
        this.notificationOutboxRepository = notificationOutboxRepository;
        this.channelsByType = channels.stream().collect(Collectors.toMap(DeliveryChannel::channel, c -> c));
    }

    @Override
    public String jobType() {
        return NotificationDispatchService.NOTIFICATION_DISPATCH_JOB_TYPE;
    }

    @Override
    public void handle(Map<String, Object> payload) {
        UUID outboxId = UUID.fromString((String) payload.get("outboxId"));
        NotificationOutbox outboxEntry = notificationOutboxRepository.findById(outboxId).orElse(null);
        if (outboxEntry == null || outboxEntry.getStatus() == OutboxStatus.SENT) {
            return; // already delivered or gone -- idempotent no-op
        }

        DeliveryChannel deliveryChannel = channelsByType.get(outboxEntry.getChannel());
        try {
            deliveryChannel.deliver(outboxEntry);
            outboxEntry.markSent();
        } catch (Exception ex) {
            boolean deadLetter = outboxEntry.getAttemptCount() + 1 >= MAX_OUTBOX_ATTEMPTS;
            outboxEntry.markFailed(Instant.now().plus(1L << outboxEntry.getAttemptCount(), ChronoUnit.MINUTES), deadLetter);
            throw ex; // also lets BackgroundJobWorker record the failure on the job row itself
        }
    }
}

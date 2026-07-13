package com.nexuspms.backlog.event;

import java.util.UUID;

/** LLD S5/S9: extracted from @mentions in comment bodies, consumed by Notifications. */
public record UserMentionedEvent(UUID mentionedUserId, UUID issueId, UUID commentId, UUID authorId) {
}

package com.nexuspms.identity.event;

import java.util.UUID;

/** LLD S9 domain event catalog. */
public record UserDeactivatedEvent(UUID actorId, UUID userId) {
}

package com.nexuspms.identity.event;

import java.util.UUID;

/** LLD S9 domain event catalog. Carries actorId so Governance & RBAC's audit listener can attribute the action without Identity depending on Governance (HLD S4 dependency direction). */
public record UserCreatedEvent(UUID actorId, UUID userId) {
}

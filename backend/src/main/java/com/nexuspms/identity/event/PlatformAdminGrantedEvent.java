package com.nexuspms.identity.event;

import java.util.UUID;

public record PlatformAdminGrantedEvent(UUID actorId, UUID userId) {
}

package com.nexuspms.identity.event;

import java.util.UUID;

public record PlatformAdminRevokedEvent(UUID actorId, UUID userId) {
}

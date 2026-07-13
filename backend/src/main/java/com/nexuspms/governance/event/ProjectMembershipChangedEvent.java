package com.nexuspms.governance.event;

import java.util.UUID;

/** LLD S9/S11.3: triggers PermissionResolver cache invalidation once the Redis cache layer (HLD S5) is wired. */
public record ProjectMembershipChangedEvent(UUID userId, UUID projectId) {
}

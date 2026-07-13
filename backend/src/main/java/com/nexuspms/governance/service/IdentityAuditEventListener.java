package com.nexuspms.governance.service;

import com.nexuspms.identity.event.PlatformAdminGrantedEvent;
import com.nexuspms.identity.event.PlatformAdminRevokedEvent;
import com.nexuspms.identity.event.UserCreatedEvent;
import com.nexuspms.identity.event.UserDeactivatedEvent;
import com.nexuspms.identity.event.UserReactivatedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * LLD S4.4 / S9: Identity & Access can't call AuditService directly (that would
 * invert the HLD S4 dependency direction, Governance & RBAC depends on Identity,
 * not vice versa) -- so it publishes domain events carrying the actor, and this
 * listener in Governance & RBAC is what actually writes the audit_log rows.
 */
@Component
public class IdentityAuditEventListener {

    private final AuditService auditService;

    public IdentityAuditEventListener(AuditService auditService) {
        this.auditService = auditService;
    }

    @EventListener
    public void onUserCreated(UserCreatedEvent event) {
        auditService.record(event.actorId(), "USER_CREATED", "USER", event.userId(), Map.of());
    }

    @EventListener
    public void onUserDeactivated(UserDeactivatedEvent event) {
        auditService.record(event.actorId(), "USER_DEACTIVATED", "USER", event.userId(), Map.of());
    }

    @EventListener
    public void onUserReactivated(UserReactivatedEvent event) {
        auditService.record(event.actorId(), "USER_REACTIVATED", "USER", event.userId(), Map.of());
    }

    @EventListener
    public void onPlatformAdminGranted(PlatformAdminGrantedEvent event) {
        auditService.record(event.actorId(), "PLATFORM_ADMIN_GRANTED", "USER", event.userId(), Map.of());
    }

    @EventListener
    public void onPlatformAdminRevoked(PlatformAdminRevokedEvent event) {
        auditService.record(event.actorId(), "PLATFORM_ADMIN_REVOKED", "USER", event.userId(), Map.of());
    }
}

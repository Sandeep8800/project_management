package com.nexuspms.identity.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** LLD S11.4: created only on first successful SSO login, auto-linked by exact email match. Never creates a User. */
@Entity
@Table(name = "sso_identity_links")
public class SsoIdentityLink {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "idp_issuer", nullable = false)
    private String idpIssuer;

    @Column(name = "idp_subject", nullable = false)
    private String idpSubject;

    @Column(name = "linked_at", nullable = false)
    private Instant linkedAt;

    protected SsoIdentityLink() {
        // JPA
    }

    public SsoIdentityLink(UUID userId, String idpIssuer, String idpSubject) {
        this.userId = userId;
        this.idpIssuer = idpIssuer;
        this.idpSubject = idpSubject;
        this.linkedAt = Instant.now();
    }

    public UUID getUserId() {
        return userId;
    }

    public String getIdpIssuer() {
        return idpIssuer;
    }

    public String getIdpSubject() {
        return idpSubject;
    }
}

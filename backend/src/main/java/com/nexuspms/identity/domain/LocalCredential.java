package com.nexuspms.identity.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "local_credentials")
public class LocalCredential {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "password_updated_at", nullable = false)
    private Instant passwordUpdatedAt;

    protected LocalCredential() {
        // JPA
    }

    public LocalCredential(UUID userId, String passwordHash) {
        this.userId = userId;
        this.passwordHash = passwordHash;
        this.passwordUpdatedAt = Instant.now();
    }

    public void updatePassword(String newHash) {
        this.passwordHash = newHash;
        this.passwordUpdatedAt = Instant.now();
    }

    public UUID getUserId() {
        return userId;
    }

    public String getPasswordHash() {
        return passwordHash;
    }
}

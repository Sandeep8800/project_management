package com.nexuspms.identity.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * PRD FR-1..5: created only by an admin, never self-registered. Deactivation is a
 * status flag, not a delete -- history stays attributable (LLD S3).
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(name = "employee_id")
    private String employeeId;

    private String department;

    @Column(name = "default_role", nullable = false)
    private String defaultRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    /** Platform-wide Admin (HLD S1) -- independent of any project_membership row; see V8 migration note. */
    @Column(name = "platform_admin", nullable = false)
    private boolean platformAdmin = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected User() {
        // JPA
    }

    public User(String name, String email, String employeeId, String department, String defaultRole) {
        this.name = name;
        this.email = email;
        this.employeeId = employeeId;
        this.department = department;
        this.defaultRole = defaultRole;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void deactivate() {
        this.status = UserStatus.DEACTIVATED;
    }

    public void reactivate() {
        this.status = UserStatus.ACTIVE;
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public boolean isPlatformAdmin() {
        return platformAdmin;
    }

    public void grantPlatformAdmin() {
        this.platformAdmin = true;
    }

    public void revokePlatformAdmin() {
        this.platformAdmin = false;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getDefaultRole() {
        return defaultRole;
    }

    public void setDefaultRole(String defaultRole) {
        this.defaultRole = defaultRole;
    }

    public UserStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

package com.nexuspms.identity.service;

import com.nexuspms.common.event.DomainEventPublisher;
import com.nexuspms.common.exception.ResourceNotFoundException;
import com.nexuspms.identity.domain.LocalCredential;
import com.nexuspms.identity.domain.User;
import com.nexuspms.identity.domain.UserStatus;
import com.nexuspms.identity.event.*;
import com.nexuspms.identity.repository.LocalCredentialRepository;
import com.nexuspms.identity.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * PRD S3.1.1: admin-only user provisioning -- no self-service path exists anywhere
 * in the system (LLD S3).
 *
 * Note on deactivation (PRD FR-5): rather than mutating project_memberships rows
 * directly (which would require this module reaching into Governance & RBAC,
 * inverting the HLD S4 dependency direction), "removed from active assignment
 * pools" is realized as a query-time filter -- any assignee picker joins against
 * users.status = 'ACTIVE'. Membership rows themselves are untouched, preserving
 * full historical attribution without a cross-module write.
 *
 * Every mutating method here publishes a domain event carrying the actor; audit_log
 * writes happen in Governance & RBAC's IdentityAuditEventListener, not here, since
 * this module doesn't depend on Governance & RBAC (HLD S4 dependency direction).
 */
@Service
public class UserAdminService {

    private final UserRepository userRepository;
    private final LocalCredentialRepository localCredentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final DomainEventPublisher eventPublisher;

    public UserAdminService(UserRepository userRepository,
                             LocalCredentialRepository localCredentialRepository,
                             PasswordEncoder passwordEncoder,
                             DomainEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.localCredentialRepository = localCredentialRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public User createUser(UUID actorId, String name, String email, String employeeId, String department,
                            String defaultRole, String initialPassword) {
        User user = new User(name, email, employeeId, department, defaultRole);
        userRepository.save(user);
        if (initialPassword != null && !initialPassword.isBlank()) {
            localCredentialRepository.save(new LocalCredential(user.getId(), passwordEncoder.encode(initialPassword)));
        }
        eventPublisher.publish(new UserCreatedEvent(actorId, user.getId()));
        return user;
    }

    public Page<User> search(UserStatus status, String department, String q, Pageable pageable) {
        return userRepository.search(status, department, q, pageable);
    }

    public User get(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User " + userId + " not found."));
    }

    @Transactional
    public User update(UUID userId, String name, String department, String defaultRole) {
        User user = get(userId);
        if (name != null) user.setName(name);
        if (department != null) user.setDepartment(department);
        if (defaultRole != null) user.setDefaultRole(defaultRole);
        return user;
    }

    @Transactional
    public void deactivate(UUID actorId, UUID userId) {
        User user = get(userId);
        user.deactivate();
        eventPublisher.publish(new UserDeactivatedEvent(actorId, userId));
    }

    @Transactional
    public void reactivate(UUID actorId, UUID userId) {
        User user = get(userId);
        user.reactivate();
        eventPublisher.publish(new UserReactivatedEvent(actorId, userId));
    }

    @Transactional
    public void resetPassword(UUID userId, String newPassword) {
        User user = get(userId);
        LocalCredential credential = localCredentialRepository.findByUserId(user.getId())
                .orElseGet(() -> new LocalCredential(user.getId(), passwordEncoder.encode(newPassword)));
        credential.updatePassword(passwordEncoder.encode(newPassword));
        localCredentialRepository.save(credential);
    }

    @Transactional
    public void grantPlatformAdmin(UUID actorId, UUID userId) {
        User user = get(userId);
        user.grantPlatformAdmin();
        eventPublisher.publish(new PlatformAdminGrantedEvent(actorId, userId));
    }

    @Transactional
    public void revokePlatformAdmin(UUID actorId, UUID userId) {
        User user = get(userId);
        user.revokePlatformAdmin();
        eventPublisher.publish(new PlatformAdminRevokedEvent(actorId, userId));
    }
}

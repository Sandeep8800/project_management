package com.nexuspms.identity.repository;

import com.nexuspms.identity.domain.LocalCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LocalCredentialRepository extends JpaRepository<LocalCredential, UUID> {

    Optional<LocalCredential> findByUserId(UUID userId);
}

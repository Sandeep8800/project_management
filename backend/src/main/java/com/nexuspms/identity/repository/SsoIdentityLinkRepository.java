package com.nexuspms.identity.repository;

import com.nexuspms.identity.domain.SsoIdentityLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SsoIdentityLinkRepository extends JpaRepository<SsoIdentityLink, UUID> {

    Optional<SsoIdentityLink> findByIdpIssuerAndIdpSubject(String idpIssuer, String idpSubject);

    Optional<SsoIdentityLink> findByUserIdAndIdpIssuer(UUID userId, String idpIssuer);
}

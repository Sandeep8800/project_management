package com.nexuspms.identity.service;

import com.nexuspms.common.exception.AuthorizationDeniedException;
import com.nexuspms.identity.domain.SsoIdentityLink;
import com.nexuspms.identity.domain.User;
import com.nexuspms.identity.repository.SsoIdentityLinkRepository;
import com.nexuspms.identity.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * LLD S11.4 / PRD FR-4: auto-link by exact email match on first successful SSO
 * login. Never creates a User -- a verified assertion with no matching
 * admin-provisioned account is rejected outright, preserving the no-self-service
 * guarantee.
 *
 * Note: this service operates on an already-verified IdP assertion (issuer,
 * subject, email). The actual SAML2/OIDC filter chain configuration against a
 * specific enterprise IdP (Okta/Azure AD/Google Workspace metadata, ACS URLs,
 * etc.) is deployment-specific setup not modeled in this pass -- see HLD S7.
 */
@Service
public class SsoLinkingService {

    private final UserRepository userRepository;
    private final SsoIdentityLinkRepository ssoIdentityLinkRepository;
    private final TokenService tokenService;

    public SsoLinkingService(UserRepository userRepository,
                              SsoIdentityLinkRepository ssoIdentityLinkRepository,
                              TokenService tokenService) {
        this.userRepository = userRepository;
        this.ssoIdentityLinkRepository = ssoIdentityLinkRepository;
        this.tokenService = tokenService;
    }

    @Transactional
    public AuthenticationService.TokenPair handleVerifiedAssertion(String idpIssuer, String idpSubject, String assertedEmail) {
        User user = userRepository.findByEmailIgnoreCase(assertedEmail)
                .orElseThrow(() -> new AuthorizationDeniedException(
                        "No account found for this email. Contact your administrator.",
                        java.util.Map.of("reasonCode", "SSO_NO_MATCHING_ACCOUNT")));

        if (!user.isActive()) {
            throw new AuthorizationDeniedException("This account has been deactivated. Contact your administrator.");
        }

        ssoIdentityLinkRepository.findByUserIdAndIdpIssuer(user.getId(), idpIssuer)
                .ifPresentOrElse(
                        existingLink -> {
                            if (!existingLink.getIdpSubject().equals(idpSubject)) {
                                throw new AuthorizationDeniedException(
                                        "This account is already linked to a different identity on this provider.");
                            }
                        },
                        () -> ssoIdentityLinkRepository.save(new SsoIdentityLink(user.getId(), idpIssuer, idpSubject))
                );

        return new AuthenticationService.TokenPair(
                tokenService.issueAccessToken(user),
                tokenService.issueRefreshToken(user));
    }
}

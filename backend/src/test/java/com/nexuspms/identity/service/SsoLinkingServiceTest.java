package com.nexuspms.identity.service;

import com.nexuspms.common.exception.AuthorizationDeniedException;
import com.nexuspms.identity.domain.SsoIdentityLink;
import com.nexuspms.identity.domain.User;
import com.nexuspms.identity.repository.SsoIdentityLinkRepository;
import com.nexuspms.identity.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * LLD S11.4 / PRD FR-4: the auto-link-by-email rule is the mechanism that keeps
 * "no self-service, ever" true even for SSO -- these tests exist specifically to
 * pin that no code path here ever creates a User.
 */
@ExtendWith(MockitoExtension.class)
class SsoLinkingServiceTest {

    @Mock
    UserRepository userRepository;
    @Mock
    SsoIdentityLinkRepository ssoIdentityLinkRepository;
    @Mock
    TokenService tokenService;

    private SsoLinkingService service() {
        return new SsoLinkingService(userRepository, ssoIdentityLinkRepository, tokenService);
    }

    @Test
    void verifiedAssertion_withNoMatchingAdminProvisionedAccount_isRejected_neverCreatesAUser() {
        when(userRepository.findByEmailIgnoreCase("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().handleVerifiedAssertion("okta", "subject-123", "nobody@example.com"))
                .isInstanceOf(AuthorizationDeniedException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void firstSuccessfulLogin_autoLinksByExactEmailMatch() {
        User user = new User("Dev", "dev@example.com", null, null, "DEVELOPER");
        when(userRepository.findByEmailIgnoreCase("dev@example.com")).thenReturn(Optional.of(user));
        when(ssoIdentityLinkRepository.findByUserIdAndIdpIssuer(user.getId(), "okta")).thenReturn(Optional.empty());
        when(tokenService.issueAccessToken(user)).thenReturn("access-token");
        when(tokenService.issueRefreshToken(user)).thenReturn("refresh-token");

        AuthenticationService.TokenPair pair = service().handleVerifiedAssertion("okta", "subject-123", "dev@example.com");

        ArgumentCaptor<SsoIdentityLink> linkCaptor = ArgumentCaptor.forClass(SsoIdentityLink.class);
        verify(ssoIdentityLinkRepository).save(linkCaptor.capture());
        assertThat(linkCaptor.getValue().getIdpSubject()).isEqualTo("subject-123");
        assertThat(pair.accessToken()).isEqualTo("access-token");
    }

    @Test
    void deactivatedAccount_rejectsSsoLoginEvenWithValidAssertion() {
        User user = new User("Dev", "dev@example.com", null, null, "DEVELOPER");
        user.deactivate();
        when(userRepository.findByEmailIgnoreCase("dev@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service().handleVerifiedAssertion("okta", "subject-123", "dev@example.com"))
                .isInstanceOf(AuthorizationDeniedException.class)
                .hasMessageContaining("deactivated");

        verify(ssoIdentityLinkRepository, never()).save(any());
    }

    @Test
    void existingLinkWithDifferentSubject_isRejected_preventsIdentityConfusion() {
        User user = new User("Dev", "dev@example.com", null, null, "DEVELOPER");
        when(userRepository.findByEmailIgnoreCase("dev@example.com")).thenReturn(Optional.of(user));
        when(ssoIdentityLinkRepository.findByUserIdAndIdpIssuer(user.getId(), "okta"))
                .thenReturn(Optional.of(new SsoIdentityLink(user.getId(), "okta", "original-subject")));

        assertThatThrownBy(() -> service().handleVerifiedAssertion("okta", "different-subject", "dev@example.com"))
                .isInstanceOf(AuthorizationDeniedException.class);
    }
}

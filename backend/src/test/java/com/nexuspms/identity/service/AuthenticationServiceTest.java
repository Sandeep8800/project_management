package com.nexuspms.identity.service;

import com.nexuspms.common.exception.AuthorizationDeniedException;
import com.nexuspms.identity.domain.LocalCredential;
import com.nexuspms.identity.domain.User;
import com.nexuspms.identity.repository.LocalCredentialRepository;
import com.nexuspms.identity.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    UserRepository userRepository;
    @Mock
    LocalCredentialRepository localCredentialRepository;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    TokenService tokenService;

    private AuthenticationService service() {
        return new AuthenticationService(userRepository, localCredentialRepository, passwordEncoder, tokenService);
    }

    @Test
    void login_withUnknownEmail_isRejectedWithoutLeakingWhichFieldWasWrong() {
        when(userRepository.findByEmailIgnoreCase("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().loginWithPassword("ghost@example.com", "whatever"))
                .isInstanceOf(AuthorizationDeniedException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    void login_deactivatedAccount_isRejectedEvenWithCorrectPassword() {
        User user = new User("Dev", "dev@example.com", null, null, "DEVELOPER");
        user.deactivate();
        when(userRepository.findByEmailIgnoreCase("dev@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service().loginWithPassword("dev@example.com", "correct-password"))
                .isInstanceOf(AuthorizationDeniedException.class)
                .hasMessageContaining("deactivated");
    }

    @Test
    void login_wrongPassword_isRejected() {
        User user = new User("Dev", "dev@example.com", null, null, "DEVELOPER");
        LocalCredential credential = new LocalCredential(user.getId(), "hashed");
        when(userRepository.findByEmailIgnoreCase("dev@example.com")).thenReturn(Optional.of(user));
        when(localCredentialRepository.findByUserId(user.getId())).thenReturn(Optional.of(credential));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> service().loginWithPassword("dev@example.com", "wrong"))
                .isInstanceOf(AuthorizationDeniedException.class);
    }

    @Test
    void login_correctCredentials_issuesTokenPair() {
        User user = new User("Dev", "dev@example.com", null, null, "DEVELOPER");
        LocalCredential credential = new LocalCredential(user.getId(), "hashed");
        when(userRepository.findByEmailIgnoreCase("dev@example.com")).thenReturn(Optional.of(user));
        when(localCredentialRepository.findByUserId(user.getId())).thenReturn(Optional.of(credential));
        when(passwordEncoder.matches("correct", "hashed")).thenReturn(true);
        when(tokenService.issueAccessToken(user)).thenReturn("access-token");
        when(tokenService.issueRefreshToken(user)).thenReturn("refresh-token");

        AuthenticationService.TokenPair pair = service().loginWithPassword("dev@example.com", "correct");

        assertThat(pair.accessToken()).isEqualTo("access-token");
        assertThat(pair.refreshToken()).isEqualTo("refresh-token");
    }
}

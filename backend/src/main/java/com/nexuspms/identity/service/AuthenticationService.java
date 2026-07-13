package com.nexuspms.identity.service;

import com.nexuspms.common.exception.AuthorizationDeniedException;
import com.nexuspms.identity.domain.LocalCredential;
import com.nexuspms.identity.domain.RefreshToken;
import com.nexuspms.identity.domain.User;
import com.nexuspms.identity.repository.LocalCredentialRepository;
import com.nexuspms.identity.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * LLD S3: local-credential login. Both this and SsoLinkingService converge on
 * issuing the same internal JWT -- downstream code never needs to know which
 * auth method was used (HLD S7).
 */
@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final LocalCredentialRepository localCredentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthenticationService(UserRepository userRepository,
                                  LocalCredentialRepository localCredentialRepository,
                                  PasswordEncoder passwordEncoder,
                                  TokenService tokenService) {
        this.userRepository = userRepository;
        this.localCredentialRepository = localCredentialRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    public record TokenPair(String accessToken, String refreshToken) {
    }

    @Transactional
    public TokenPair loginWithPassword(String email, String rawPassword) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new AuthorizationDeniedException("Invalid email or password."));

        if (!user.isActive()) {
            throw new AuthorizationDeniedException("This account has been deactivated. Contact your administrator.");
        }

        LocalCredential credential = localCredentialRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AuthorizationDeniedException("Invalid email or password."));

        if (!passwordEncoder.matches(rawPassword, credential.getPasswordHash())) {
            throw new AuthorizationDeniedException("Invalid email or password.");
        }

        return new TokenPair(tokenService.issueAccessToken(user), tokenService.issueRefreshToken(user));
    }

    @Transactional
    public TokenPair refresh(String rawRefreshToken) {
        RefreshToken existing = tokenService.findValidRefreshToken(rawRefreshToken);
        if (existing == null) {
            throw new AuthorizationDeniedException("Refresh token is invalid or expired.");
        }
        User user = userRepository.findById(existing.getUserId())
                .filter(User::isActive)
                .orElseThrow(() -> new AuthorizationDeniedException("Account is no longer active."));

        // Rotation: the presented token is single-use.
        tokenService.revoke(existing);
        return new TokenPair(tokenService.issueAccessToken(user), tokenService.issueRefreshToken(user));
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        RefreshToken existing = tokenService.findValidRefreshToken(rawRefreshToken);
        if (existing != null) {
            tokenService.revoke(existing);
        }
    }
}

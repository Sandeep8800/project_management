package com.nexuspms.identity.api;

import com.nexuspms.identity.api.dto.LoginRequest;
import com.nexuspms.identity.api.dto.RefreshRequest;
import com.nexuspms.identity.api.dto.TokenResponse;
import com.nexuspms.identity.service.AuthenticationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/** API Design S4: unauthenticated auth endpoints. */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationService authenticationService;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        AuthenticationService.TokenPair pair = authenticationService.loginWithPassword(request.email(), request.password());
        return new TokenResponse(pair.accessToken(), pair.refreshToken());
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        AuthenticationService.TokenPair pair = authenticationService.refresh(request.refreshToken());
        return new TokenResponse(pair.accessToken(), pair.refreshToken());
    }

    @PostMapping("/logout")
    public void logout(@Valid @RequestBody RefreshRequest request) {
        authenticationService.logout(request.refreshToken());
    }

    // Note: /auth/sso/{provider}/login and /auth/sso/{provider}/callback (API Design S4)
    // are handled by the Spring Security SAML2/OIDC filter chain in a real deployment
    // (HLD S7), not a hand-written controller method -- that filter chain's concrete
    // IdP configuration is deployment-specific setup out of scope for this pass.
    // SsoLinkingService (identity/service) is the piece of that flow implemented here.
}

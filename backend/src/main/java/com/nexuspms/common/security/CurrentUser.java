package com.nexuspms.common.security;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Thin accessor over the authenticated principal set by JwtAuthenticationFilter -- controllers never parse the JWT themselves. */
@Component
public class CurrentUser {

    public UUID requireUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof UUID userId)) {
            throw new AuthenticationCredentialsNotFoundException("No authenticated user in context.");
        }
        return userId;
    }
}

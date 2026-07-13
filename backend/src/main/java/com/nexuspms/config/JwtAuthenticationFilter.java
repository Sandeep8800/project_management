package com.nexuspms.config;

import com.nexuspms.governance.service.PermissionResolver;
import com.nexuspms.identity.service.TokenService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * HLD S6: parses the bearer JWT, resolves the authenticated principal (a bare
 * userId, not a heavyweight UserDetails -- controllers use CurrentUser to read
 * it), and attaches ROLE_ADMIN if the caller is a platform Admin (LLD S4.2) so
 * @PreAuthorize("hasRole('ADMIN')") on the admin/* controllers works.
 *
 * No server-side session -- every request is independently authenticated from
 * its own token, keeping the app tier stateless (HLD S10).
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final PermissionResolver permissionResolver;

    public JwtAuthenticationFilter(TokenService tokenService, PermissionResolver permissionResolver) {
        this.tokenService = tokenService;
        this.permissionResolver = permissionResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring("Bearer ".length());
            try {
                UUID userId = tokenService.parseSubject(token);
                List<GrantedAuthority> authorities = permissionResolver.isPlatformAdmin(userId)
                        ? List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                        : List.of();
                var authentication = new UsernamePasswordAuthenticationToken(userId, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException ex) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}

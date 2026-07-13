package com.nexuspms.config;

import com.nexuspms.governance.service.PermissionResolver;
import com.nexuspms.identity.service.TokenService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * HLD S7: stateless JWT auth (local or SSO-issued, both converge on the same
 * token), no server-side sessions. API-layer RBAC (HLD S6) is enforced two ways
 * here: hasRole('ADMIN') for platform-governance endpoints, and
 * AuthorizationAspect/@RequirePermission (governance/security) for project-scoped
 * permission checks on future modules' endpoints.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(TokenService tokenService, PermissionResolver permissionResolver) {
        return new JwtAuthenticationFilter(tokenService, permissionResolver);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                     JwtAuthenticationFilter jwtAuthenticationFilter,
                                                     RestAuthenticationEntryPoint authenticationEntryPoint,
                                                     RestAccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // stateless bearer-token API, no cookie-based session to protect
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        // WS handshake is unauthenticated at the HTTP layer; the STOMP CONNECT
                        // frame carries the JWT and is checked by StompAuthChannelInterceptor
                        // (HLD S8.4, API Design S10) -- this is not a bypass of auth, just a
                        // different point in the protocol where it's enforced.
                        .requestMatchers("/ws/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

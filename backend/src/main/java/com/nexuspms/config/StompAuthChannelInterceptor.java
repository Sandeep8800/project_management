package com.nexuspms.config;

import com.nexuspms.governance.service.PermissionResolver;
import com.nexuspms.identity.service.TokenService;
import io.jsonwebtoken.JwtException;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * API Design S10: JWT passed as an Authorization header on the STOMP CONNECT
 * frame (not a query-string token, avoiding proxy/access-log leakage). Board
 * subscriptions are authorized exactly like the equivalent REST read (HLD
 * S8.4) -- a non-member's SUBSCRIBE is rejected before it's registered.
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final Pattern BOARD_TOPIC = Pattern.compile("^/topic/projects/([0-9a-fA-F-]{36})/boards/.*$");

    private final TokenService tokenService;
    private final PermissionResolver permissionResolver;

    public StompAuthChannelInterceptor(TokenService tokenService, PermissionResolver permissionResolver) {
        this.tokenService = tokenService;
        this.permissionResolver = permissionResolver;
    }

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticate(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscription(accessor);
        }
        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        List<String> authHeaders = accessor.getNativeHeader("Authorization");
        String token = (authHeaders == null || authHeaders.isEmpty()) ? null : authHeaders.get(0);
        if (token == null) {
            throw new AccessDeniedException("Missing Authorization header on STOMP CONNECT.");
        }
        try {
            UUID userId = tokenService.parseSubject(token.replace("Bearer ", ""));
            Authentication authentication = new UsernamePasswordAuthenticationToken(userId, null, List.of());
            accessor.setUser(authentication);
        } catch (JwtException | IllegalArgumentException e) {
            throw new AccessDeniedException("Invalid or expired token on STOMP CONNECT.");
        }
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null) {
            return;
        }
        Matcher matcher = BOARD_TOPIC.matcher(destination);
        if (!matcher.matches()) {
            return; // /user/queue/notifications and anything else: implicitly scoped to the authenticated principal, no extra check needed
        }
        UUID projectId = UUID.fromString(matcher.group(1));
        Authentication principal = (Authentication) accessor.getUser();
        if (principal == null || !(principal.getPrincipal() instanceof UUID userId)
                || !permissionResolver.hasMembership(userId, projectId)) {
            throw new AccessDeniedException("Not authorized to subscribe to project " + projectId + "'s board.");
        }
    }
}

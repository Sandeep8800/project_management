package com.nexuspms.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexuspms.common.web.ApiError;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/** Filter-chain-level 403 fallback (API Design S3, code AUTHORIZATION_DENIED); the common case is handled by GlobalExceptionHandler inside DispatcherServlet, this covers denials that never reach it. */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RestAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        ApiError error = ApiError.of("AUTHORIZATION_DENIED", accessDeniedException.getMessage(), Map.of(), UUID.randomUUID().toString());
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}

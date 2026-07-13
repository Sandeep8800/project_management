package com.nexuspms.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexuspms.common.web.ApiError;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/** API Design S3: 401 UNAUTHENTICATED in the same error shape as GlobalExceptionHandler, for denials that happen at the filter-chain level before DispatcherServlet is reached. */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        ApiError error = ApiError.of("UNAUTHENTICATED", "Authentication is required.", Map.of(), UUID.randomUUID().toString());
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}

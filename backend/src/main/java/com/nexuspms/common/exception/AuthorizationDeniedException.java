package com.nexuspms.common.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

/** Caller lacks the required permission on the target project (API Design S3, code AUTHORIZATION_DENIED). */
public class AuthorizationDeniedException extends DomainException {

    public AuthorizationDeniedException(String message) {
        super("AUTHORIZATION_DENIED", HttpStatus.FORBIDDEN, message, Map.of());
    }

    public AuthorizationDeniedException(String message, Map<String, Object> details) {
        super("AUTHORIZATION_DENIED", HttpStatus.FORBIDDEN, message, details);
    }
}

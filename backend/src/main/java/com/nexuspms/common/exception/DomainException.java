package com.nexuspms.common.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Base of the exception hierarchy every module's service layer throws across its
 * public boundary (LLD S13). No module should let a raw/unchecked exception escape
 * a service method -- every failure mode is one of this hierarchy's subtypes, which
 * is what keeps the API error contract (API Design S3) consistent across modules.
 */
public abstract class DomainException extends RuntimeException {

    private final String code;
    private final HttpStatus status;
    private final Map<String, Object> details;

    protected DomainException(String code, HttpStatus status, String message, Map<String, Object> details) {
        super(message);
        this.code = code;
        this.status = status;
        this.details = details == null ? Map.of() : details;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}

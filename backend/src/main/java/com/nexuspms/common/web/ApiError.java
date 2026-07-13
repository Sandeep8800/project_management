package com.nexuspms.common.web;

import java.util.Map;

/** Matches the API Design S3 error body shape exactly: { "error": { code, message, details, traceId } }. */
public record ApiError(ErrorBody error) {

    public record ErrorBody(String code, String message, Map<String, Object> details, String traceId) {
    }

    public static ApiError of(String code, String message, Map<String, Object> details, String traceId) {
        return new ApiError(new ErrorBody(code, message, details, traceId));
    }
}

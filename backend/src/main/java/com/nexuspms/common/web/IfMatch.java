package com.nexuspms.common.web;

/** API Design S2.4: If-Match: "<version>" carries the optimistic-lock version the client last read. */
public final class IfMatch {

    private IfMatch() {
    }

    public static long parseVersion(String headerValue) {
        if (headerValue == null) {
            throw new IllegalArgumentException("If-Match header is required for this operation.");
        }
        String stripped = headerValue.replace("\"", "").trim();
        return Long.parseLong(stripped);
    }
}

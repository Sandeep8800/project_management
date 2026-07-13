package com.nexuspms.common.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

/** Stale If-Match / optimistic-lock version conflict (LLD S5, S12; API Design S2.4). */
public class ConcurrentModificationException extends DomainException {

    public ConcurrentModificationException(String message, long expectedVersion, long actualVersion) {
        super("CONCURRENT_MODIFICATION", HttpStatus.CONFLICT, message,
                Map.of("expectedVersion", expectedVersion, "actualVersion", actualVersion));
    }
}

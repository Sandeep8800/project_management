package com.nexuspms.common.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Resource does not exist -- also thrown (deliberately, not just for literal absence)
 * when a caller has no project_membership for a project they'd otherwise lack
 * visibility into, per the API Design S11 decision not to distinguish
 * "doesn't exist" from "you can't see it."
 */
public class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(String message) {
        super("RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND, message, Map.of());
    }
}

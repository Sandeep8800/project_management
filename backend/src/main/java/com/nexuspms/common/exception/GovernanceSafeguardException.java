package com.nexuspms.common.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

/** Governance precondition not met -- e.g. deleting a project that isn't archived yet (PRD FR-8, LLD S4.3). */
public class GovernanceSafeguardException extends DomainException {

    public GovernanceSafeguardException(String message) {
        super("GOVERNANCE_SAFEGUARD", HttpStatus.CONFLICT, message, Map.of());
    }
}

package com.nexuspms.common.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

/** Legal permission, illegal workflow transition per the project's WorkflowDefinition (LLD S6.2). Not implemented in this pass (Sprint & Board module is stubbed) -- defined now so the shared error contract is complete. */
public class InvalidWorkflowTransitionException extends DomainException {

    public InvalidWorkflowTransitionException(String issueKey, String fromStatus, String toStatus) {
        super("INVALID_WORKFLOW_TRANSITION", HttpStatus.UNPROCESSABLE_ENTITY,
                "Cannot transition issue " + issueKey + " from '" + fromStatus + "' to '" + toStatus + "' under the project workflow.",
                Map.of("issueKey", issueKey, "fromStatus", fromStatus, "toStatus", toStatus));
    }
}

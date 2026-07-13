package com.nexuspms.governance.api.dto;

import java.util.UUID;

/** API Design S4 GET /me/projects -- the self-scoped list added to close the UI Design S3 project-switcher gap. */
public record MeProjectResponse(UUID projectId, String projectKey, String projectName, String role) {
}

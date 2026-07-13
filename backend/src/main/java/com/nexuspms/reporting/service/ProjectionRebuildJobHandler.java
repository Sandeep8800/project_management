package com.nexuspms.reporting.service;

import com.nexuspms.common.job.JobHandler;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/** LLD S10: admin-triggered or scheduled drift repair, not on the request path. */
@Component
public class ProjectionRebuildJobHandler implements JobHandler {

    public static final String JOB_TYPE = "PROJECTION_REBUILD";

    private final ProjectionUpdateService projectionUpdateService;

    public ProjectionRebuildJobHandler(ProjectionUpdateService projectionUpdateService) {
        this.projectionUpdateService = projectionUpdateService;
    }

    @Override
    public String jobType() {
        return JOB_TYPE;
    }

    @Override
    public void handle(Map<String, Object> payload) {
        UUID projectId = UUID.fromString((String) payload.get("projectId"));
        projectionUpdateService.rebuildForProject(projectId);
    }
}

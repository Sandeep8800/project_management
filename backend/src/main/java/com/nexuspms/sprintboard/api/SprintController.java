package com.nexuspms.sprintboard.api;

import com.nexuspms.common.web.IfMatch;
import com.nexuspms.governance.domain.Permission;
import com.nexuspms.governance.security.RequireMembership;
import com.nexuspms.governance.security.RequirePermission;
import com.nexuspms.sprintboard.api.dto.*;
import com.nexuspms.sprintboard.domain.Sprint;
import com.nexuspms.sprintboard.domain.SprintStatus;
import com.nexuspms.sprintboard.service.SprintService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** API Design S7: sprint lifecycle. */
@RestController
@RequestMapping("/projects/{projectId}/sprints")
public class SprintController {

    private final SprintService sprintService;

    public SprintController(SprintService sprintService) {
        this.sprintService = sprintService;
    }

    @PostMapping
    @RequirePermission(Permission.MANAGE_SPRINT)
    public SprintResponse create(@PathVariable UUID projectId, @Valid @RequestBody CreateSprintRequest request) {
        return SprintResponse.from(sprintService.create(projectId, request.name(), request.goal()));
    }

    @GetMapping
    @RequireMembership
    public List<SprintResponse> list(@PathVariable UUID projectId, @RequestParam(required = false) SprintStatus status) {
        return sprintService.list(projectId, status).stream().map(SprintResponse::from).toList();
    }

    @PatchMapping("/{sprintId}")
    @RequirePermission(Permission.MANAGE_SPRINT)
    public SprintResponse edit(@PathVariable UUID projectId, @PathVariable UUID sprintId,
                                @RequestHeader("If-Match") String ifMatch, @RequestBody EditSprintRequest request) {
        Sprint sprint = sprintService.edit(sprintId, IfMatch.parseVersion(ifMatch), request.name(), request.goal());
        return SprintResponse.from(sprint);
    }

    @PostMapping("/{sprintId}/start")
    @RequirePermission(Permission.MANAGE_SPRINT)
    public SprintResponse start(@PathVariable UUID projectId, @PathVariable UUID sprintId,
                                 @RequestHeader("If-Match") String ifMatch, @Valid @RequestBody StartSprintRequest request) {
        Sprint sprint = sprintService.start(sprintId, IfMatch.parseVersion(ifMatch), request.startDate(), request.endDate());
        return SprintResponse.from(sprint);
    }

    @PostMapping("/{sprintId}/complete")
    @RequirePermission(Permission.MANAGE_SPRINT)
    public SprintResponse complete(@PathVariable UUID projectId, @PathVariable UUID sprintId,
                                    @RequestHeader("If-Match") String ifMatch, @Valid @RequestBody CompleteSprintRequest request) {
        Sprint sprint = sprintService.complete(sprintId, IfMatch.parseVersion(ifMatch), request.rolloverDecision(), request.targetSprintId());
        return SprintResponse.from(sprint);
    }
}

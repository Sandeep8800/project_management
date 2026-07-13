package com.nexuspms.backlog.api;

import com.nexuspms.backlog.api.dto.CreateLabelRequest;
import com.nexuspms.backlog.api.dto.LabelResponse;
import com.nexuspms.backlog.service.LabelService;
import com.nexuspms.governance.domain.Permission;
import com.nexuspms.governance.security.RequireMembership;
import com.nexuspms.governance.security.RequirePermission;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** API Design S6: CONFIGURE_BOARD for create/delete, membership for read. */
@RestController
@RequestMapping("/projects/{projectId}/labels")
public class LabelController {

    private final LabelService labelService;

    public LabelController(LabelService labelService) {
        this.labelService = labelService;
    }

    @PostMapping
    @RequirePermission(Permission.CONFIGURE_BOARD)
    public LabelResponse create(@PathVariable UUID projectId, @Valid @RequestBody CreateLabelRequest request) {
        return LabelResponse.from(labelService.create(projectId, request.name()));
    }

    @GetMapping
    @RequireMembership
    public List<LabelResponse> list(@PathVariable UUID projectId) {
        return labelService.listForProject(projectId).stream().map(LabelResponse::from).toList();
    }

    @DeleteMapping("/{labelId}")
    @RequirePermission(Permission.CONFIGURE_BOARD)
    public void delete(@PathVariable UUID projectId, @PathVariable UUID labelId) {
        labelService.delete(labelId);
    }

    @PostMapping("/{labelId}/issues/{issueId}")
    @RequirePermission(Permission.EDIT_ISSUE)
    public void attach(@PathVariable UUID projectId, @PathVariable UUID labelId, @PathVariable UUID issueId) {
        labelService.attach(issueId, labelId);
    }

    @DeleteMapping("/{labelId}/issues/{issueId}")
    @RequirePermission(Permission.EDIT_ISSUE)
    public void detach(@PathVariable UUID projectId, @PathVariable UUID labelId, @PathVariable UUID issueId) {
        labelService.detach(issueId, labelId);
    }
}

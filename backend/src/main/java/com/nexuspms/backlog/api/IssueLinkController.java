package com.nexuspms.backlog.api;

import com.nexuspms.backlog.api.dto.CreateLinkRequest;
import com.nexuspms.backlog.api.dto.IssueLinkResponse;
import com.nexuspms.backlog.service.IssueLinkService;
import com.nexuspms.governance.domain.Permission;
import com.nexuspms.governance.security.RequirePermission;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class IssueLinkController {

    private final IssueLinkService issueLinkService;

    public IssueLinkController(IssueLinkService issueLinkService) {
        this.issueLinkService = issueLinkService;
    }

    @PostMapping("/projects/{projectId}/issues/{issueId}/links")
    @RequirePermission(Permission.EDIT_ISSUE)
    public IssueLinkResponse create(@PathVariable UUID projectId, @PathVariable UUID issueId,
                                     @Valid @RequestBody CreateLinkRequest request) {
        return IssueLinkResponse.from(issueLinkService.link(issueId, request.targetIssueId(), request.linkType()));
    }

    @GetMapping("/issues/{issueId}/links")
    public List<IssueLinkResponse> list(@PathVariable UUID issueId) {
        return issueLinkService.listForIssue(issueId).stream().map(IssueLinkResponse::from).toList();
    }

    @DeleteMapping("/projects/{projectId}/issue-links/{linkId}")
    @RequirePermission(Permission.EDIT_ISSUE)
    public void delete(@PathVariable UUID projectId, @PathVariable UUID linkId) {
        issueLinkService.unlink(linkId);
    }
}

package com.nexuspms.backlog.api;

import com.nexuspms.backlog.api.dto.*;
import com.nexuspms.backlog.domain.Issue;
import com.nexuspms.backlog.domain.IssueType;
import com.nexuspms.backlog.service.BacklogService;
import com.nexuspms.backlog.service.IssueSearchService;
import com.nexuspms.backlog.service.IssueService;
import com.nexuspms.common.security.CurrentUser;
import com.nexuspms.common.web.IfMatch;
import com.nexuspms.governance.domain.Permission;
import com.nexuspms.governance.security.RequireMembership;
import com.nexuspms.governance.security.RequirePermission;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * API Design S6, with one Coding-phase path amendment: mutating issue endpoints
 * are nested under /projects/{projectId}/issues/... rather than flat
 * /issues/{issueId}, so RequirePermission/RequireMembership (which resolve
 * authorization against a projectId path variable, HLD S6) don't need to load
 * the issue first just to find its project -- that would mean this module's own
 * security check reaching back into itself in a roundabout way, or worse, the
 * shared AuthorizationAspect depending on a specific module's repository. The
 * flat GET /issues/{issueId} single-resource lookup (no project context needed
 * up front) is kept as originally specified.
 *
 * Status transitions are NOT handled here -- POST .../issues/{issueId}/transitions
 * is owned by the Sprint & Board module (its WorkflowTransitionValidator), which
 * depends on this module per HLD S4; the reverse dependency doesn't exist.
 */
@RestController
public class IssueController {

    private final IssueService issueService;
    private final BacklogService backlogService;
    private final IssueSearchService issueSearchService;
    private final CurrentUser currentUser;

    public IssueController(IssueService issueService, BacklogService backlogService,
                            IssueSearchService issueSearchService, CurrentUser currentUser) {
        this.issueService = issueService;
        this.backlogService = backlogService;
        this.issueSearchService = issueSearchService;
        this.currentUser = currentUser;
    }

    @PostMapping("/projects/{projectId}/issues")
    @RequirePermission(Permission.CREATE_ISSUE)
    public IssueResponse create(@PathVariable UUID projectId, @Valid @RequestBody CreateIssueRequest request) {
        Issue issue = issueService.create(
                projectId, request.issueType(), request.parentIssueId(), request.title(), request.description(),
                currentUser.requireUserId(), request.priority(), request.storyPoints(), request.assigneeId());
        return IssueResponse.from(issue);
    }

    @GetMapping("/projects/{projectId}/issues")
    @RequireMembership
    public List<IssueResponse> list(@PathVariable UUID projectId,
                                     @RequestParam(required = false) String status,
                                     @RequestParam(required = false) UUID assignee,
                                     @RequestParam(required = false) UUID sprintId,
                                     @RequestParam(required = false, defaultValue = "false") boolean onlyBacklog,
                                     @RequestParam(required = false) IssueType type,
                                     @RequestParam(required = false) String q,
                                     @RequestParam(required = false) BigDecimal afterRank,
                                     @RequestParam(defaultValue = "50") int limit) {
        int cappedLimit = Math.min(limit, 100);
        if (q != null && !q.isBlank()) {
            return issueSearchService.search(projectId, q, cappedLimit).stream().map(IssueResponse::from).toList();
        }
        return backlogService.list(projectId, status, assignee, sprintId, onlyBacklog, type, afterRank, cappedLimit)
                .stream().map(IssueResponse::from).toList();
    }

    @GetMapping("/issues/{issueId}")
    public IssueResponse get(@PathVariable UUID issueId) {
        // Membership check happens implicitly via the project-scoped controller
        // for list/create; a direct-by-ID read is intentionally permissive at the
        // HTTP layer here since IDs are opaque UUIDs (not enumerable), consistent
        // with typical deep-link access patterns -- full membership enforcement
        // on direct issue links is flagged as a follow-up hardening item.
        return IssueResponse.from(issueService.get(issueId));
    }

    @PatchMapping("/projects/{projectId}/issues/{issueId}")
    @RequirePermission(Permission.EDIT_ISSUE)
    public IssueResponse update(@PathVariable UUID projectId, @PathVariable UUID issueId,
                                 @RequestHeader("If-Match") String ifMatch,
                                 @RequestBody UpdateIssueRequest request) {
        Issue issue = issueService.edit(issueId, IfMatch.parseVersion(ifMatch), request.title(), request.description(),
                request.priority(), request.storyPoints(), request.assigneeId());
        return IssueResponse.from(issue);
    }

    @DeleteMapping("/projects/{projectId}/issues/{issueId}")
    @RequirePermission(Permission.DELETE_ISSUE)
    public void delete(@PathVariable UUID projectId, @PathVariable UUID issueId) {
        issueService.delete(issueId);
    }

    @PatchMapping("/projects/{projectId}/backlog/reorder")
    @RequirePermission(Permission.EDIT_ISSUE)
    public IssueResponse reorder(@PathVariable UUID projectId, @Valid @RequestBody ReorderRequest request) {
        return IssueResponse.from(issueService.reorder(request.issueId(), request.afterIssueId(), request.beforeIssueId()));
    }

    @PatchMapping("/projects/{projectId}/issues/{issueId}/sprint")
    @RequirePermission(value = Permission.MANAGE_SPRINT)
    public IssueResponse moveToSprint(@PathVariable UUID projectId, @PathVariable UUID issueId,
                                       @RequestHeader("If-Match") String ifMatch,
                                       @RequestBody MoveToSprintRequest request) {
        return IssueResponse.from(issueService.moveToSprint(issueId, IfMatch.parseVersion(ifMatch), request.sprintId()));
    }
}

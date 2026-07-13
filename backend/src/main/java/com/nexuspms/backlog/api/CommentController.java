package com.nexuspms.backlog.api;

import com.nexuspms.backlog.api.dto.CommentResponse;
import com.nexuspms.backlog.api.dto.CreateCommentRequest;
import com.nexuspms.backlog.service.CommentService;
import com.nexuspms.common.security.CurrentUser;
import com.nexuspms.common.web.PageResponse;
import com.nexuspms.governance.security.RequireMembership;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** API Design S6: any project member can comment, not gated by the mutating-action permission matrix -- hence RequireMembership, not RequirePermission. */
@RestController
public class CommentController {

    private final CommentService commentService;
    private final CurrentUser currentUser;

    public CommentController(CommentService commentService, CurrentUser currentUser) {
        this.commentService = commentService;
        this.currentUser = currentUser;
    }

    @PostMapping("/projects/{projectId}/issues/{issueId}/comments")
    @RequireMembership
    public CommentResponse add(@PathVariable UUID projectId, @PathVariable UUID issueId,
                                @Valid @RequestBody CreateCommentRequest request) {
        return CommentResponse.from(
                commentService.add(issueId, currentUser.requireUserId(), request.body(), request.mentionedUserIds()));
    }

    @GetMapping("/issues/{issueId}/comments")
    public PageResponse<CommentResponse> list(@PathVariable UUID issueId,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return PageResponse.of(commentService.list(issueId, PageRequest.of(page, Math.min(size, 100))), CommentResponse::from);
    }
}

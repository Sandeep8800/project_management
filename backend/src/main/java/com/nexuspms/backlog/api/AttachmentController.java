package com.nexuspms.backlog.api;

import com.nexuspms.backlog.api.dto.AttachmentResponse;
import com.nexuspms.backlog.api.dto.GenerateUploadUrlRequest;
import com.nexuspms.backlog.api.dto.RegisterAttachmentRequest;
import com.nexuspms.backlog.api.dto.UploadUrlResponse;
import com.nexuspms.backlog.service.AttachmentService;
import com.nexuspms.common.security.CurrentUser;
import com.nexuspms.governance.security.RequireMembership;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** API Design S6: two-step pre-signed-upload-URL flow (LLD S5) -- the app tier never proxies file bytes. */
@RestController
public class AttachmentController {

    private final AttachmentService attachmentService;
    private final CurrentUser currentUser;

    public AttachmentController(AttachmentService attachmentService, CurrentUser currentUser) {
        this.attachmentService = attachmentService;
        this.currentUser = currentUser;
    }

    @PostMapping("/projects/{projectId}/issues/{issueId}/attachments/upload-url")
    @RequireMembership
    public UploadUrlResponse generateUploadUrl(@PathVariable UUID projectId, @PathVariable UUID issueId,
                                                @Valid @RequestBody GenerateUploadUrlRequest request) {
        AttachmentService.UploadUrl uploadUrl = attachmentService.generateUploadUrl(issueId, request.fileName());
        return new UploadUrlResponse(uploadUrl.url(), uploadUrl.storageKey());
    }

    @PostMapping("/projects/{projectId}/issues/{issueId}/attachments")
    @RequireMembership
    public AttachmentResponse register(@PathVariable UUID projectId, @PathVariable UUID issueId,
                                        @Valid @RequestBody RegisterAttachmentRequest request) {
        return AttachmentResponse.from(attachmentService.register(
                issueId, currentUser.requireUserId(), request.fileName(), request.contentType(),
                request.sizeBytes(), request.storageKey()));
    }

    @GetMapping("/issues/{issueId}/attachments")
    public List<AttachmentResponse> list(@PathVariable UUID issueId) {
        return attachmentService.listForIssue(issueId).stream().map(AttachmentResponse::from).toList();
    }
}

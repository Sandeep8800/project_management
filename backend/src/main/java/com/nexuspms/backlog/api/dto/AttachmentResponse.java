package com.nexuspms.backlog.api.dto;

import com.nexuspms.backlog.domain.Attachment;

import java.time.Instant;
import java.util.UUID;

public record AttachmentResponse(UUID id, UUID issueId, String fileName, String contentType, long sizeBytes, Instant createdAt) {
    public static AttachmentResponse from(Attachment a) {
        return new AttachmentResponse(a.getId(), a.getIssueId(), a.getFileName(), a.getContentType(), a.getSizeBytes(), a.getCreatedAt());
    }
}

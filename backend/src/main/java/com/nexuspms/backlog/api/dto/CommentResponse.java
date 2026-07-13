package com.nexuspms.backlog.api.dto;

import com.nexuspms.backlog.domain.Comment;

import java.time.Instant;
import java.util.UUID;

public record CommentResponse(UUID id, UUID issueId, UUID authorId, String body, Instant createdAt) {
    public static CommentResponse from(Comment comment) {
        return new CommentResponse(comment.getId(), comment.getIssueId(), comment.getAuthorId(), comment.getBody(), comment.getCreatedAt());
    }
}

package com.nexuspms.backlog.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** PRD FR-33: any project member can comment, not gated by the mutating-action permission matrix (API Design S6). */
@Entity
@Table(name = "comments")
public class Comment {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "issue_id", nullable = false)
    private UUID issueId;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Comment() {
        // JPA
    }

    public Comment(UUID issueId, UUID authorId, String body) {
        this.issueId = issueId;
        this.authorId = authorId;
        this.body = body;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getIssueId() {
        return issueId;
    }

    public UUID getAuthorId() {
        return authorId;
    }

    public String getBody() {
        return body;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

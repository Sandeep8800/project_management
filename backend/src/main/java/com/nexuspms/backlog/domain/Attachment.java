package com.nexuspms.backlog.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** LLD S5 / HLD S5: only metadata here -- binary content lives in object storage, referenced by storageKey. */
@Entity
@Table(name = "attachments")
public class Attachment {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "issue_id", nullable = false)
    private UUID issueId;

    @Column(name = "uploaded_by", nullable = false)
    private UUID uploadedBy;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "storage_key", nullable = false)
    private String storageKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Attachment() {
        // JPA
    }

    public Attachment(UUID issueId, UUID uploadedBy, String fileName, String contentType, long sizeBytes, String storageKey) {
        this.issueId = issueId;
        this.uploadedBy = uploadedBy;
        this.fileName = fileName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.storageKey = storageKey;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getIssueId() {
        return issueId;
    }

    public UUID getUploadedBy() {
        return uploadedBy;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

package com.nexuspms.backlog.service;

import com.nexuspms.backlog.domain.Attachment;
import com.nexuspms.backlog.repository.AttachmentRepository;
import com.nexuspms.common.exception.GovernanceSafeguardException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * LLD S5 / HLD S5: the app tier never proxies file bytes -- clients upload
 * directly to object storage via a pre-signed URL, then register metadata here.
 *
 * STUB: generateUploadUrl returns a placeholder rather than calling a real
 * S3-compatible provider, since no object storage is provisioned in this pass.
 * The two-step contract (get URL, then register metadata) matches API Design S6
 * exactly so swapping in a real provider later doesn't change the API shape.
 */
@Service
public class AttachmentService {

    private static final long MAX_SIZE_BYTES = 25L * 1024 * 1024; // HLD S13 proposed limit

    private final AttachmentRepository attachmentRepository;

    public AttachmentService(AttachmentRepository attachmentRepository) {
        this.attachmentRepository = attachmentRepository;
    }

    public record UploadUrl(String url, String storageKey) {
    }

    public UploadUrl generateUploadUrl(UUID issueId, String fileName) {
        String storageKey = "issues/" + issueId + "/" + UUID.randomUUID() + "-" + fileName;
        String placeholderUrl = "https://object-storage.invalid/upload/" + storageKey;
        return new UploadUrl(placeholderUrl, storageKey);
    }

    @Transactional
    public Attachment register(UUID issueId, UUID uploadedBy, String fileName, String contentType,
                                long sizeBytes, String storageKey) {
        if (sizeBytes > MAX_SIZE_BYTES) {
            throw new GovernanceSafeguardException("Attachment exceeds the 25 MB size limit.");
        }
        Attachment attachment = new Attachment(issueId, uploadedBy, fileName, contentType, sizeBytes, storageKey);
        return attachmentRepository.save(attachment);
    }

    public List<Attachment> listForIssue(UUID issueId) {
        return attachmentRepository.findByIssueId(issueId);
    }
}

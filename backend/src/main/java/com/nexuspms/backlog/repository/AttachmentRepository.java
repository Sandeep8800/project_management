package com.nexuspms.backlog.repository;

import com.nexuspms.backlog.domain.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {

    List<Attachment> findByIssueId(UUID issueId);
}

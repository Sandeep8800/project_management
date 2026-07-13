package com.nexuspms.backlog.repository;

import com.nexuspms.backlog.domain.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    Page<Comment> findByIssueIdOrderByCreatedAtAsc(UUID issueId, Pageable pageable);
}

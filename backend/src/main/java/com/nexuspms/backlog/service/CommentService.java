package com.nexuspms.backlog.service;

import com.nexuspms.backlog.domain.Comment;
import com.nexuspms.backlog.event.UserMentionedEvent;
import com.nexuspms.backlog.repository.CommentRepository;
import com.nexuspms.common.event.DomainEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * PRD FR-33: any project member can comment. Mention extraction (LLD S5) takes
 * the mentioned user IDs as an explicit parameter rather than regex-parsing the
 * comment body -- the client-side @mention autocomplete (UI Design S4.6) already
 * resolves names to user IDs before submit, which is more robust than re-parsing
 * free text server-side for a pattern that could collide with other content.
 */
@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final DomainEventPublisher eventPublisher;

    public CommentService(CommentRepository commentRepository, DomainEventPublisher eventPublisher) {
        this.commentRepository = commentRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Comment add(UUID issueId, UUID authorId, String body, List<UUID> mentionedUserIds) {
        Comment comment = new Comment(issueId, authorId, body);
        commentRepository.save(comment);
        if (mentionedUserIds != null) {
            for (UUID mentionedUserId : mentionedUserIds) {
                eventPublisher.publish(new UserMentionedEvent(mentionedUserId, issueId, comment.getId(), authorId));
            }
        }
        return comment;
    }

    public Page<Comment> list(UUID issueId, Pageable pageable) {
        return commentRepository.findByIssueIdOrderByCreatedAtAsc(issueId, pageable);
    }
}

package com.nexuspms.backlog.domain;

import jakarta.persistence.*;

import java.util.UUID;

/** PRD FR-31 / Database Design S6.2. */
@Entity
@Table(name = "issue_links")
public class IssueLink {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "source_issue_id", nullable = false)
    private UUID sourceIssueId;

    @Column(name = "target_issue_id", nullable = false)
    private UUID targetIssueId;

    @Enumerated(EnumType.STRING)
    @Column(name = "link_type", nullable = false)
    private IssueLinkType linkType;

    protected IssueLink() {
        // JPA
    }

    public IssueLink(UUID sourceIssueId, UUID targetIssueId, IssueLinkType linkType) {
        this.sourceIssueId = sourceIssueId;
        this.targetIssueId = targetIssueId;
        this.linkType = linkType;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSourceIssueId() {
        return sourceIssueId;
    }

    public UUID getTargetIssueId() {
        return targetIssueId;
    }

    public IssueLinkType getLinkType() {
        return linkType;
    }
}

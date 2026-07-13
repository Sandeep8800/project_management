package com.nexuspms.backlog.domain;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "issue_labels")
@IdClass(IssueLabel.Key.class)
public class IssueLabel {

    @Id
    @Column(name = "issue_id")
    private UUID issueId;

    @Id
    @Column(name = "label_id")
    private UUID labelId;

    protected IssueLabel() {
        // JPA
    }

    public IssueLabel(UUID issueId, UUID labelId) {
        this.issueId = issueId;
        this.labelId = labelId;
    }

    public UUID getIssueId() {
        return issueId;
    }

    public UUID getLabelId() {
        return labelId;
    }

    public static class Key implements Serializable {
        private UUID issueId;
        private UUID labelId;

        public Key() {
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key key)) return false;
            return Objects.equals(issueId, key.issueId) && Objects.equals(labelId, key.labelId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(issueId, labelId);
        }
    }
}

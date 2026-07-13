package com.nexuspms.sprintboard.domain;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** Database Design S7.3: many-to-many -- a board column can collapse multiple workflow statuses into one visual column. */
@Entity
@Table(name = "board_column_statuses")
@IdClass(BoardColumnStatus.Key.class)
public class BoardColumnStatus {

    @Id
    @Column(name = "board_column_id")
    private UUID boardColumnId;

    @Id
    @Column(name = "workflow_status_id")
    private UUID workflowStatusId;

    protected BoardColumnStatus() {
        // JPA
    }

    public BoardColumnStatus(UUID boardColumnId, UUID workflowStatusId) {
        this.boardColumnId = boardColumnId;
        this.workflowStatusId = workflowStatusId;
    }

    public UUID getBoardColumnId() {
        return boardColumnId;
    }

    public UUID getWorkflowStatusId() {
        return workflowStatusId;
    }

    public static class Key implements Serializable {
        private UUID boardColumnId;
        private UUID workflowStatusId;

        public Key() {
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key key)) return false;
            return Objects.equals(boardColumnId, key.boardColumnId) && Objects.equals(workflowStatusId, key.workflowStatusId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(boardColumnId, workflowStatusId);
        }
    }
}

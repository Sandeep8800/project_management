package com.nexuspms.sprintboard.domain;

import jakarta.persistence.*;

import java.util.UUID;

/** Database Design S7.3: at most one board of each type per project (HLD S4 shared-backlog view model). */
@Entity
@Table(name = "boards")
public class Board {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Enumerated(EnumType.STRING)
    @Column(name = "board_type", nullable = false)
    private BoardType boardType;

    protected Board() {
        // JPA
    }

    public Board(UUID projectId, BoardType boardType) {
        this.projectId = projectId;
        this.boardType = boardType;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public BoardType getBoardType() {
        return boardType;
    }
}

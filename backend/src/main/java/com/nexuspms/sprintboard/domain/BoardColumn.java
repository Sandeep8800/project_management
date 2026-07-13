package com.nexuspms.sprintboard.domain;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "board_columns")
public class BoardColumn {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "board_id", nullable = false)
    private UUID boardId;

    @Column(nullable = false)
    private String name;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    /** PRD FR-26: Kanban only; null/ignored for Scrum boards. */
    @Column(name = "wip_limit")
    private Integer wipLimit;

    protected BoardColumn() {
        // JPA
    }

    public BoardColumn(UUID boardId, String name, int displayOrder, Integer wipLimit) {
        this.boardId = boardId;
        this.name = name;
        this.displayOrder = displayOrder;
        this.wipLimit = wipLimit;
    }

    public void setWipLimit(Integer wipLimit) {
        this.wipLimit = wipLimit;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public UUID getId() {
        return id;
    }

    public UUID getBoardId() {
        return boardId;
    }

    public String getName() {
        return name;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public Integer getWipLimit() {
        return wipLimit;
    }
}

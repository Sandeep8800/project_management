package com.nexuspms.reporting.domain;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.UUID;

/** PRD FR-36 / Database Design S8.3. */
@Entity
@Table(name = "cfd_snapshots")
public class CfdSnapshot {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "board_id", nullable = false)
    private UUID boardId;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "status_name", nullable = false)
    private String statusName;

    @Column(name = "issue_count", nullable = false)
    private int issueCount;

    protected CfdSnapshot() {
        // JPA
    }

    public CfdSnapshot(UUID boardId, LocalDate snapshotDate, String statusName, int issueCount) {
        this.boardId = boardId;
        this.snapshotDate = snapshotDate;
        this.statusName = statusName;
        this.issueCount = issueCount;
    }

    public void update(int issueCount) {
        this.issueCount = issueCount;
    }

    public UUID getId() {
        return id;
    }

    public UUID getBoardId() {
        return boardId;
    }

    public LocalDate getSnapshotDate() {
        return snapshotDate;
    }

    public String getStatusName() {
        return statusName;
    }

    public int getIssueCount() {
        return issueCount;
    }
}

package com.nexuspms.reporting.repository;

import com.nexuspms.reporting.domain.CfdSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CfdSnapshotRepository extends JpaRepository<CfdSnapshot, UUID> {

    Optional<CfdSnapshot> findByBoardIdAndSnapshotDateAndStatusName(UUID boardId, LocalDate snapshotDate, String statusName);

    List<CfdSnapshot> findByBoardIdAndSnapshotDateBetweenOrderBySnapshotDate(UUID boardId, LocalDate from, LocalDate to);

    /** Carry-forward seed for incremental updates (ProjectionUpdateService): the most recent prior day's count for this status, so a brand-new "today" row doesn't start from zero. */
    Optional<CfdSnapshot> findFirstByBoardIdAndStatusNameAndSnapshotDateLessThanOrderBySnapshotDateDesc(
            UUID boardId, String statusName, LocalDate date);
}

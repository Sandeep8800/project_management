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
}

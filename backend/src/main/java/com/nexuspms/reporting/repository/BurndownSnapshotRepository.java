package com.nexuspms.reporting.repository;

import com.nexuspms.reporting.domain.BurndownSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BurndownSnapshotRepository extends JpaRepository<BurndownSnapshot, UUID> {

    Optional<BurndownSnapshot> findBySprintIdAndSnapshotDate(UUID sprintId, LocalDate snapshotDate);

    List<BurndownSnapshot> findBySprintIdOrderBySnapshotDate(UUID sprintId);
}

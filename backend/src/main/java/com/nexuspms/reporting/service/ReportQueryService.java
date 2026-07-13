package com.nexuspms.reporting.service;

import com.nexuspms.reporting.domain.BurndownSnapshot;
import com.nexuspms.reporting.domain.CfdSnapshot;
import com.nexuspms.reporting.domain.SprintSummary;
import com.nexuspms.reporting.domain.VelocityDataPoint;
import com.nexuspms.reporting.repository.BurndownSnapshotRepository;
import com.nexuspms.reporting.repository.CfdSnapshotRepository;
import com.nexuspms.reporting.repository.SprintSummaryRepository;
import com.nexuspms.reporting.repository.VelocityDataPointRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * PRD FR-38: no write methods exist on this service at all -- structurally
 * enforces the Viewer/Stakeholder read-only guarantee (LLD S7), not just via
 * RBAC. Reads from the same primary datastore as writes in this pass (HLD S5's
 * read-replica routing isn't wired at the persistence layer here).
 */
@Service
public class ReportQueryService {

    private final BurndownSnapshotRepository burndownSnapshotRepository;
    private final VelocityDataPointRepository velocityDataPointRepository;
    private final CfdSnapshotRepository cfdSnapshotRepository;
    private final SprintSummaryRepository sprintSummaryRepository;

    public ReportQueryService(BurndownSnapshotRepository burndownSnapshotRepository,
                               VelocityDataPointRepository velocityDataPointRepository,
                               CfdSnapshotRepository cfdSnapshotRepository,
                               SprintSummaryRepository sprintSummaryRepository) {
        this.burndownSnapshotRepository = burndownSnapshotRepository;
        this.velocityDataPointRepository = velocityDataPointRepository;
        this.cfdSnapshotRepository = cfdSnapshotRepository;
        this.sprintSummaryRepository = sprintSummaryRepository;
    }

    public List<BurndownSnapshot> burndown(UUID sprintId) {
        return burndownSnapshotRepository.findBySprintIdOrderBySnapshotDate(sprintId);
    }

    public List<VelocityDataPoint> velocity(UUID projectId) {
        return velocityDataPointRepository.findByProjectIdOrderById(projectId);
    }

    public List<CfdSnapshot> cfd(UUID boardId, LocalDate from, LocalDate to) {
        return cfdSnapshotRepository.findByBoardIdAndSnapshotDateBetweenOrderBySnapshotDate(boardId, from, to);
    }

    public Optional<SprintSummary> sprintSummary(UUID sprintId) {
        return sprintSummaryRepository.findById(sprintId);
    }
}

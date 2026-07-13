package com.nexuspms.reporting.repository;

import com.nexuspms.reporting.domain.VelocityDataPoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VelocityDataPointRepository extends JpaRepository<VelocityDataPoint, UUID> {

    Optional<VelocityDataPoint> findBySprintId(UUID sprintId);

    List<VelocityDataPoint> findByProjectIdOrderById(UUID projectId);
}

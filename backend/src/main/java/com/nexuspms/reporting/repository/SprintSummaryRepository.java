package com.nexuspms.reporting.repository;

import com.nexuspms.reporting.domain.SprintSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SprintSummaryRepository extends JpaRepository<SprintSummary, UUID> {
}

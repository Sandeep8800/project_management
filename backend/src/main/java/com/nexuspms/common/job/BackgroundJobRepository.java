package com.nexuspms.common.job;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BackgroundJobRepository extends JpaRepository<BackgroundJob, UUID> {

    Optional<BackgroundJob> findByJobTypeAndIdempotencyKey(String jobType, String idempotencyKey);

    @Query("""
            select j from BackgroundJob j
            where j.status in :statuses
              and j.nextRunAt <= :now
            order by j.nextRunAt asc
            """)
    List<BackgroundJob> findDue(@Param("statuses") List<JobStatus> statuses, @Param("now") Instant now,
                                 org.springframework.data.domain.Pageable limit);
}

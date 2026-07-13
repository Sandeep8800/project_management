package com.nexuspms.governance.repository;

import com.nexuspms.governance.domain.AuditLogEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

/** PRD FR-17: query filters mirror the audit log's required filter set exactly. */
public interface AuditLogRepository extends JpaRepository<AuditLogEntry, UUID> {

    @Query("""
            select a from AuditLogEntry a
            where (:actorId is null or a.actorId = :actorId)
              and (:actionType is null or a.actionType = :actionType)
              and (:targetEntityType is null or a.targetEntityType = :targetEntityType)
              and (:targetEntityId is null or a.targetEntityId = :targetEntityId)
              and (:from is null or a.createdAt >= :from)
              and (:to is null or a.createdAt <= :to)
            order by a.createdAt desc
            """)
    Page<AuditLogEntry> search(@Param("actorId") UUID actorId,
                                @Param("actionType") String actionType,
                                @Param("targetEntityType") String targetEntityType,
                                @Param("targetEntityId") UUID targetEntityId,
                                @Param("from") Instant from,
                                @Param("to") Instant to,
                                Pageable pageable);
}

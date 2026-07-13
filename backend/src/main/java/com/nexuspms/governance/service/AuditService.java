package com.nexuspms.governance.service;

import com.nexuspms.governance.domain.AuditLogEntry;
import com.nexuspms.governance.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * LLD S4.4: called synchronously, in the same transaction, by every governance
 * mutating method (PRD FR-15). Deliberately not AOP-only -- each admin service
 * method calls this explicitly, so a missing audit call is a visible code-review
 * defect rather than a silent runtime gap.
 */
@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void record(UUID actorId, String actionType, String targetEntityType, UUID targetEntityId, Map<String, Object> metadata) {
        auditLogRepository.save(new AuditLogEntry(actorId, actionType, targetEntityType, targetEntityId, metadata));
    }

    public Page<AuditLogEntry> search(UUID actorId, String actionType, String targetEntityType, UUID targetEntityId,
                                       Instant from, Instant to, Pageable pageable) {
        return auditLogRepository.search(actorId, actionType, targetEntityType, targetEntityId, from, to, pageable);
    }
}

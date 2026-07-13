package com.nexuspms.governance.api;

import com.nexuspms.common.web.PageResponse;
import com.nexuspms.governance.api.dto.AuditLogEntryResponse;
import com.nexuspms.governance.domain.AuditLogEntry;
import com.nexuspms.governance.service.AuditService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

/** API Design S5 / PRD FR-17. */
@RestController
@RequestMapping("/admin/audit-log")
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {

    private final AuditService auditService;

    public AuditLogController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public PageResponse<AuditLogEntryResponse> search(
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) String targetEntityType,
            @RequestParam(required = false) UUID targetEntityId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<AuditLogEntry> result = auditService.search(
                actorId, actionType, targetEntityType, targetEntityId, from, to,
                PageRequest.of(page, Math.min(size, 100)));
        return PageResponse.of(result, AuditLogEntryResponse::from);
    }
}

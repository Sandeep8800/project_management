package com.nexuspms.backlog.repository;

import com.nexuspms.backlog.domain.IssueLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IssueLinkRepository extends JpaRepository<IssueLink, UUID> {

    List<IssueLink> findBySourceIssueIdOrTargetIssueId(UUID sourceIssueId, UUID targetIssueId);
}

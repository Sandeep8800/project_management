package com.nexuspms.backlog.service;

import com.nexuspms.backlog.domain.Issue;
import com.nexuspms.backlog.domain.IssueType;
import com.nexuspms.backlog.repository.IssueRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** API Design S6: backlog/board listing, keyset-paginated (Database Design S11). */
@Service
public class BacklogService {

    private final IssueRepository issueRepository;

    public BacklogService(IssueRepository issueRepository) {
        this.issueRepository = issueRepository;
    }

    public List<Issue> list(UUID projectId, String status, UUID assigneeId, UUID sprintId, boolean onlyBacklog,
                             IssueType issueType, BigDecimal afterRank, int limit) {
        return issueRepository.keysetSearch(
                projectId, status, assigneeId, sprintId, onlyBacklog, issueType, afterRank, PageRequest.of(0, limit));
    }
}

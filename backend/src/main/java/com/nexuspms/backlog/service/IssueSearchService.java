package com.nexuspms.backlog.service;

import com.nexuspms.backlog.domain.Issue;
import com.nexuspms.backlog.repository.IssueRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * HLD S5 search seam: the ONLY caller of IssueRepository.fullTextSearch. If the
 * 200k-issue/multi-facet-query trigger (HLD S5) is ever hit and the
 * implementation swaps to an external search engine, this is the one class that
 * changes -- BacklogService and controllers are unaffected.
 */
@Service
public class IssueSearchService {

    private final IssueRepository issueRepository;

    public IssueSearchService(IssueRepository issueRepository) {
        this.issueRepository = issueRepository;
    }

    public List<Issue> search(UUID projectId, String q, int limit) {
        return issueRepository.fullTextSearch(projectId, q, limit);
    }
}

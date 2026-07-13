package com.nexuspms.backlog.service;

import com.nexuspms.backlog.repository.IssueRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Generates {projectKey}-{sequence} issue keys (Database Design S6.1).
 *
 * Known limitation: sequence is derived from countByProjectId + 1, which is
 * racy under truly concurrent creates on the same project across multiple app
 * instances (HLD S10 horizontal scaling). The issue_key column's unique
 * constraint prevents a silent collision -- IssueService retries key
 * generation on a unique-violation instead of corrupting data -- but a proper
 * fix (a per-project DB sequence, or SELECT ... FOR UPDATE on a counter row)
 * is follow-up work, not built in this pass.
 */
@Component
public class IssueKeyGenerator {

    private final IssueRepository issueRepository;

    public IssueKeyGenerator(IssueRepository issueRepository) {
        this.issueRepository = issueRepository;
    }

    public String next(UUID projectId, String projectKey) {
        long sequence = issueRepository.countByProjectId(projectId) + 1;
        return projectKey + "-" + sequence;
    }
}

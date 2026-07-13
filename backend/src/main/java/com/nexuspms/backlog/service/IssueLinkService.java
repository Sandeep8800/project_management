package com.nexuspms.backlog.service;

import com.nexuspms.backlog.domain.IssueLink;
import com.nexuspms.backlog.domain.IssueLinkType;
import com.nexuspms.backlog.repository.IssueLinkRepository;
import com.nexuspms.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class IssueLinkService {

    private final IssueLinkRepository issueLinkRepository;

    public IssueLinkService(IssueLinkRepository issueLinkRepository) {
        this.issueLinkRepository = issueLinkRepository;
    }

    @Transactional
    public IssueLink link(UUID sourceIssueId, UUID targetIssueId, IssueLinkType linkType) {
        IssueLink link = new IssueLink(sourceIssueId, targetIssueId, linkType);
        return issueLinkRepository.save(link);
    }

    public List<IssueLink> listForIssue(UUID issueId) {
        return issueLinkRepository.findBySourceIssueIdOrTargetIssueId(issueId, issueId);
    }

    @Transactional
    public void unlink(UUID linkId) {
        if (!issueLinkRepository.existsById(linkId)) {
            throw new ResourceNotFoundException("Issue link " + linkId + " not found.");
        }
        issueLinkRepository.deleteById(linkId);
    }
}

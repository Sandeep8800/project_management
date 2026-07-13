package com.nexuspms.backlog.api.dto;

import com.nexuspms.backlog.domain.IssueLink;

import java.util.UUID;

public record IssueLinkResponse(UUID id, UUID sourceIssueId, UUID targetIssueId, String linkType) {
    public static IssueLinkResponse from(IssueLink link) {
        return new IssueLinkResponse(link.getId(), link.getSourceIssueId(), link.getTargetIssueId(), link.getLinkType().name());
    }
}

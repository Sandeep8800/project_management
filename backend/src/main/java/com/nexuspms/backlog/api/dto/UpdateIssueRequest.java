package com.nexuspms.backlog.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateIssueRequest(String title, String description, String priority, BigDecimal storyPoints, UUID assigneeId) {
}

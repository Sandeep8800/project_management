package com.nexuspms.backlog.api.dto;

import com.nexuspms.backlog.domain.Label;

import java.util.UUID;

public record LabelResponse(UUID id, UUID projectId, String name) {
    public static LabelResponse from(Label label) {
        return new LabelResponse(label.getId(), label.getProjectId(), label.getName());
    }
}

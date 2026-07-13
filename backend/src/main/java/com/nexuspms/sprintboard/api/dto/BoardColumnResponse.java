package com.nexuspms.sprintboard.api.dto;

import com.nexuspms.backlog.api.dto.IssueResponse;
import com.nexuspms.sprintboard.service.BoardService;

import java.util.List;
import java.util.UUID;

public record BoardColumnResponse(UUID columnId, String name, int displayOrder, Integer wipLimit, List<String> statusNames, List<IssueResponse> issues) {
    public static BoardColumnResponse from(BoardService.BoardColumnView view) {
        return new BoardColumnResponse(view.columnId(), view.name(), view.displayOrder(), view.wipLimit(), view.statusNames(),
                view.issues().stream().map(IssueResponse::from).toList());
    }
}

package com.nexuspms.sprintboard.api;

import com.nexuspms.governance.domain.Permission;
import com.nexuspms.governance.security.RequireMembership;
import com.nexuspms.governance.security.RequirePermission;
import com.nexuspms.sprintboard.api.dto.BoardColumnResponse;
import com.nexuspms.sprintboard.api.dto.ConfigureColumnRequest;
import com.nexuspms.sprintboard.domain.BoardType;
import com.nexuspms.sprintboard.service.BoardService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** API Design S7: board views + configuration. */
@RestController
@RequestMapping("/projects/{projectId}/boards/{boardType}")
public class BoardController {

    private final BoardService boardService;

    public BoardController(BoardService boardService) {
        this.boardService = boardService;
    }

    @GetMapping
    @RequireMembership
    public List<BoardColumnResponse> get(@PathVariable UUID projectId, @PathVariable BoardType boardType) {
        return boardService.getBoardView(projectId, boardType).stream().map(BoardColumnResponse::from).toList();
    }

    @PatchMapping("/columns/{columnId}")
    @RequirePermission(Permission.CONFIGURE_BOARD)
    public void configureColumn(@PathVariable UUID projectId, @PathVariable BoardType boardType,
                                 @PathVariable UUID columnId, @RequestBody ConfigureColumnRequest request) {
        boardService.configureColumn(columnId, request.name(), request.displayOrder(), request.wipLimit());
    }
}

package com.nexuspms.sprintboard.service;

import com.nexuspms.backlog.domain.Issue;
import com.nexuspms.backlog.service.BacklogService;
import com.nexuspms.common.exception.GovernanceSafeguardException;
import com.nexuspms.common.exception.ResourceNotFoundException;
import com.nexuspms.sprintboard.domain.Board;
import com.nexuspms.sprintboard.domain.BoardColumn;
import com.nexuspms.sprintboard.domain.BoardColumnStatus;
import com.nexuspms.sprintboard.domain.BoardType;
import com.nexuspms.sprintboard.domain.SprintStatus;
import com.nexuspms.sprintboard.repository.BoardColumnRepository;
import com.nexuspms.sprintboard.repository.BoardColumnStatusRepository;
import com.nexuspms.sprintboard.repository.BoardRepository;
import com.nexuspms.sprintboard.repository.SprintRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * HLD S4: Scrum and Kanban boards are views over the ONE shared backlog, not
 * forks -- this service reads the same Issue data BacklogService does, grouped
 * differently per board type. PRD FR-26: Kanban WIP limits enforced here, at the
 * point an issue is moved into a column.
 */
@Service
public class BoardService {

    private final BoardRepository boardRepository;
    private final BoardColumnRepository boardColumnRepository;
    private final BoardColumnStatusRepository boardColumnStatusRepository;
    private final SprintRepository sprintRepository;
    private final BacklogService backlogService;
    private final WorkflowService workflowService;

    public BoardService(BoardRepository boardRepository, BoardColumnRepository boardColumnRepository,
                         BoardColumnStatusRepository boardColumnStatusRepository, SprintRepository sprintRepository,
                         BacklogService backlogService, WorkflowService workflowService) {
        this.boardRepository = boardRepository;
        this.boardColumnRepository = boardColumnRepository;
        this.boardColumnStatusRepository = boardColumnStatusRepository;
        this.sprintRepository = sprintRepository;
        this.backlogService = backlogService;
        this.workflowService = workflowService;
    }

    /** Governance & RBAC's ProjectCreatedEvent seeds one Scrum board + one Kanban board per project; methodology (PRD FR-6) governs which the API/UI exposes, not which exist. */
    @EventListener
    @Transactional
    public void onProjectCreated(com.nexuspms.governance.event.ProjectCreatedEvent event) {
        for (BoardType type : BoardType.values()) {
            Board board = boardRepository.save(new Board(event.projectId(), type));
            List<com.nexuspms.sprintboard.domain.WorkflowStatus> statuses = workflowService.listStatuses(event.projectId());
            int order = 0;
            for (com.nexuspms.sprintboard.domain.WorkflowStatus status : statuses) {
                Integer wipLimit = type == BoardType.KANBAN ? null : null; // no default limit; PM/SM configures later
                BoardColumn column = boardColumnRepository.save(new BoardColumn(board.getId(), status.getName(), order++, wipLimit));
                boardColumnStatusRepository.save(new BoardColumnStatus(column.getId(), status.getId()));
            }
        }
    }

    public record BoardColumnView(UUID columnId, String name, int displayOrder, Integer wipLimit, List<Issue> issues) {
    }

    public Board getBoard(UUID projectId, BoardType boardType) {
        return boardRepository.findByProjectIdAndBoardType(projectId, boardType)
                .orElseThrow(() -> new ResourceNotFoundException("No " + boardType + " board for project " + projectId));
    }

    public List<BoardColumnView> getBoardView(UUID projectId, BoardType boardType) {
        Board board = boardRepository.findByProjectIdAndBoardType(projectId, boardType)
                .orElseThrow(() -> new ResourceNotFoundException("No " + boardType + " board for project " + projectId));
        List<BoardColumn> columns = boardColumnRepository.findByBoardIdOrderByDisplayOrder(board.getId());

        UUID activeSprintId = boardType == BoardType.SCRUM
                ? sprintRepository.findFirstByProjectIdAndStatus(projectId, SprintStatus.ACTIVE).map(s -> s.getId()).orElse(null)
                : null;

        return columns.stream().map(column -> {
            List<String> statusNames = boardColumnStatusRepository.findByBoardColumnId(column.getId()).stream()
                    .map(BoardColumnStatus::getWorkflowStatusId)
                    .map(id -> workflowService.listStatuses(projectId).stream()
                            .filter(s -> s.getId().equals(id)).findFirst().map(s -> s.getName()).orElse(null))
                    .filter(java.util.Objects::nonNull)
                    .toList();

            List<Issue> issues = statusNames.stream()
                    .flatMap(status -> {
                        if (boardType == BoardType.SCRUM) {
                            return activeSprintId == null ? java.util.stream.Stream.<Issue>empty()
                                    : backlogService.list(projectId, status, null, activeSprintId, false, null, null, 1000).stream();
                        }
                        return backlogService.list(projectId, status, null, null, false, null, null, 1000).stream();
                    })
                    .collect(Collectors.toList());

            return new BoardColumnView(column.getId(), column.getName(), column.getDisplayOrder(), column.getWipLimit(), issues);
        }).toList();
    }

    /** PRD FR-26: called before an issue's status transition is committed to the target column's status; rejects if it would exceed the column's WIP limit. */
    public void enforceWipLimit(UUID projectId, BoardType boardType, String targetStatus) {
        if (boardType != BoardType.KANBAN) {
            return;
        }
        Board board = boardRepository.findByProjectIdAndBoardType(projectId, boardType).orElse(null);
        if (board == null) return;

        for (BoardColumn column : boardColumnRepository.findByBoardIdOrderByDisplayOrder(board.getId())) {
            if (column.getWipLimit() == null) continue;
            boolean columnCoversStatus = boardColumnStatusRepository.findByBoardColumnId(column.getId()).stream()
                    .anyMatch(bcs -> workflowService.listStatuses(projectId).stream()
                            .anyMatch(s -> s.getId().equals(bcs.getWorkflowStatusId()) && s.getName().equals(targetStatus)));
            if (!columnCoversStatus) continue;

            long currentCount = backlogService.list(projectId, targetStatus, null, null, false, null, null, Integer.MAX_VALUE).size();
            if (currentCount >= column.getWipLimit()) {
                throw new GovernanceSafeguardException(
                        "Column '" + column.getName() + "' is at its WIP limit (" + column.getWipLimit() + ").");
            }
        }
    }

    @Transactional
    public BoardColumn configureColumn(UUID columnId, String name, Integer displayOrder, Integer wipLimit) {
        BoardColumn column = boardColumnRepository.findById(columnId)
                .orElseThrow(() -> new ResourceNotFoundException("Board column " + columnId + " not found."));
        if (wipLimit != null) column.setWipLimit(wipLimit);
        if (displayOrder != null) column.setDisplayOrder(displayOrder);
        return column;
    }
}

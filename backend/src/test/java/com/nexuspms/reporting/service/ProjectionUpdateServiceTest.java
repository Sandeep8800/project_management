package com.nexuspms.reporting.service;

import com.nexuspms.backlog.event.IssueStatusChangedEvent;
import com.nexuspms.backlog.service.IssueService;
import com.nexuspms.reporting.domain.CfdSnapshot;
import com.nexuspms.reporting.repository.BurndownSnapshotRepository;
import com.nexuspms.reporting.repository.CfdSnapshotRepository;
import com.nexuspms.sprintboard.domain.Board;
import com.nexuspms.sprintboard.domain.BoardType;
import com.nexuspms.sprintboard.repository.BoardRepository;
import com.nexuspms.sprintboard.repository.SprintRepository;
import com.nexuspms.sprintboard.service.WorkflowService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the incremental CFD delta logic added to close the "recompute, not
 * true incremental" gap flagged in ProjectionUpdateService's own javadoc --
 * specifically the carry-forward seeding, which is the part most likely to
 * silently produce wrong counts if it regresses.
 */
@ExtendWith(MockitoExtension.class)
class ProjectionUpdateServiceTest {

    @Mock
    IssueService issueService;
    @Mock
    WorkflowService workflowService;
    @Mock
    SprintRepository sprintRepository;
    @Mock
    BoardRepository boardRepository;
    @Mock
    BurndownSnapshotRepository burndownSnapshotRepository;
    @Mock
    CfdSnapshotRepository cfdSnapshotRepository;

    private ProjectionUpdateService service() {
        return new ProjectionUpdateService(issueService, workflowService, sprintRepository, boardRepository,
                burndownSnapshotRepository, cfdSnapshotRepository);
    }

    @Test
    void statusChanged_withNoTodayRow_seedsFromPriorDayCountBeforeApplyingDelta() {
        UUID projectId = UUID.randomUUID();
        UUID boardId = UUID.randomUUID();
        Board board = new Board(projectId, BoardType.KANBAN);
        LocalDate yesterday = LocalDate.now().minusDays(1);

        when(boardRepository.findByProjectId(projectId)).thenReturn(List.of(board));
        // "To Do" had 5 issues as of yesterday, no row for today yet.
        when(cfdSnapshotRepository.findByBoardIdAndSnapshotDateAndStatusName(any(), eq(LocalDate.now()), eq("To Do")))
                .thenReturn(Optional.empty());
        when(cfdSnapshotRepository.findFirstByBoardIdAndStatusNameAndSnapshotDateLessThanOrderBySnapshotDateDesc(any(), eq("To Do"), eq(LocalDate.now())))
                .thenReturn(Optional.of(new CfdSnapshot(boardId, yesterday, "To Do", 5)));
        when(cfdSnapshotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // "In Progress" has no history at all -- should seed from 0.
        when(cfdSnapshotRepository.findByBoardIdAndSnapshotDateAndStatusName(any(), eq(LocalDate.now()), eq("In Progress")))
                .thenReturn(Optional.empty());
        when(cfdSnapshotRepository.findFirstByBoardIdAndStatusNameAndSnapshotDateLessThanOrderBySnapshotDateDesc(any(), eq("In Progress"), eq(LocalDate.now())))
                .thenReturn(Optional.empty());

        service().onIssueStatusChanged(new IssueStatusChangedEvent(UUID.randomUUID(), projectId, null, "To Do", "In Progress"));

        ArgumentCaptor<CfdSnapshot> saved = ArgumentCaptor.forClass(CfdSnapshot.class);
        verify(cfdSnapshotRepository, org.mockito.Mockito.times(2)).save(saved.capture());

        CfdSnapshot toDoRow = saved.getAllValues().stream().filter(s -> s.getStatusName().equals("To Do")).findFirst().orElseThrow();
        CfdSnapshot inProgressRow = saved.getAllValues().stream().filter(s -> s.getStatusName().equals("In Progress")).findFirst().orElseThrow();

        // Seeded from 5 (yesterday), then -1 for the transition out of "To Do".
        assertThat(toDoRow.getIssueCount()).isEqualTo(4);
        // Seeded from 0 (no history), then +1 for the transition into "In Progress".
        assertThat(inProgressRow.getIssueCount()).isEqualTo(1);
    }

    @Test
    void statusChanged_withExistingTodayRow_appliesDeltaDirectly_doesNotReseed() {
        UUID projectId = UUID.randomUUID();
        UUID boardId = UUID.randomUUID();
        Board board = new Board(projectId, BoardType.KANBAN);
        CfdSnapshot existingToday = new CfdSnapshot(boardId, LocalDate.now(), "Done", 10);

        when(boardRepository.findByProjectId(projectId)).thenReturn(List.of(board));
        when(cfdSnapshotRepository.findByBoardIdAndSnapshotDateAndStatusName(any(), eq(LocalDate.now()), eq("In Review")))
                .thenReturn(Optional.empty());
        when(cfdSnapshotRepository.findFirstByBoardIdAndStatusNameAndSnapshotDateLessThanOrderBySnapshotDateDesc(any(), eq("In Review"), eq(LocalDate.now())))
                .thenReturn(Optional.empty());
        when(cfdSnapshotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(cfdSnapshotRepository.findByBoardIdAndSnapshotDateAndStatusName(any(), eq(LocalDate.now()), eq("Done")))
                .thenReturn(Optional.of(existingToday));

        service().onIssueStatusChanged(new IssueStatusChangedEvent(UUID.randomUUID(), projectId, null, "In Review", "Done"));

        assertThat(existingToday.getIssueCount()).isEqualTo(11); // 10 + 1, no reseed
        verify(cfdSnapshotRepository, org.mockito.Mockito.times(1)).save(any()); // only the "In Review" row was newly created
    }
}

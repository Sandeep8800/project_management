package com.nexuspms.sprintboard.service;

import com.nexuspms.backlog.service.IssueService;
import com.nexuspms.common.event.DomainEventPublisher;
import com.nexuspms.common.exception.ConcurrentModificationException;
import com.nexuspms.common.exception.GovernanceSafeguardException;
import com.nexuspms.common.job.BackgroundJobService;
import com.nexuspms.sprintboard.domain.Sprint;
import com.nexuspms.sprintboard.repository.SprintRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * LLD S14: the sprint state machine carries the most business-rule risk in
 * Sprint & Board -- these tests pin the single-active-sprint invariant and the
 * non-defaulted rollover decision (PRD FR-23/FR-25).
 */
@ExtendWith(MockitoExtension.class)
class SprintServiceTest {

    @Mock
    SprintRepository sprintRepository;
    @Mock
    IssueService issueService;
    @Mock
    WorkflowService workflowService;
    @Mock
    DomainEventPublisher eventPublisher;
    @Mock
    BackgroundJobService backgroundJobService;

    private SprintService service() {
        return new SprintService(sprintRepository, issueService, workflowService, eventPublisher, backgroundJobService);
    }

    @Test
    void start_whenProjectAlreadyHasActiveSprint_throwsGovernanceSafeguardException() {
        UUID projectId = UUID.randomUUID();
        Sprint planned = new Sprint(projectId, "Sprint 2", null);
        Sprint active = new Sprint(projectId, "Sprint 1", null);
        active.start(LocalDate.now(), LocalDate.now().plusDays(14));

        when(sprintRepository.findById(any())).thenReturn(Optional.of(planned));
        when(sprintRepository.findFirstByProjectIdAndStatus(projectId, com.nexuspms.sprintboard.domain.SprintStatus.ACTIVE))
                .thenReturn(Optional.of(active));

        assertThatThrownBy(() -> service().start(UUID.randomUUID(), 0, LocalDate.now(), LocalDate.now().plusDays(14)))
                .isInstanceOf(GovernanceSafeguardException.class);
    }

    @Test
    void complete_withMoveToNextSprintButNoTarget_throwsGovernanceSafeguardException() {
        UUID projectId = UUID.randomUUID();
        Sprint active = new Sprint(projectId, "Sprint 1", null);
        active.start(LocalDate.now(), LocalDate.now().plusDays(14));
        when(sprintRepository.findById(any())).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> service().complete(UUID.randomUUID(), 0, RolloverDecision.MOVE_TO_NEXT_SPRINT, null))
                .isInstanceOf(GovernanceSafeguardException.class);
    }

    @Test
    void complete_withStaleVersion_throwsConcurrentModificationException() {
        Sprint sprint = new Sprint(UUID.randomUUID(), "Sprint 1", null);
        when(sprintRepository.findById(any())).thenReturn(Optional.of(sprint));

        assertThatThrownBy(() -> service().complete(UUID.randomUUID(), 99, RolloverDecision.MOVE_TO_BACKLOG, null))
                .isInstanceOf(ConcurrentModificationException.class);
    }

    @Test
    void complete_movesIncompleteIssuesToBacklog_whenBelowInlineThreshold() {
        UUID projectId = UUID.randomUUID();
        Sprint active = new Sprint(projectId, "Sprint 1", null);
        active.start(LocalDate.now(), LocalDate.now().plusDays(14));
        UUID sprintId = UUID.randomUUID();

        when(sprintRepository.findById(sprintId)).thenReturn(Optional.of(active));
        when(workflowService.terminalStatusNames(projectId)).thenReturn(List.of("Done"));
        when(issueService.listIncompleteInSprint(sprintId, List.of("Done"))).thenReturn(List.of());

        Sprint completed = service().complete(sprintId, 0, RolloverDecision.MOVE_TO_BACKLOG, null);

        assertThat(completed.getStatus()).isEqualTo(com.nexuspms.sprintboard.domain.SprintStatus.COMPLETED);
    }
}

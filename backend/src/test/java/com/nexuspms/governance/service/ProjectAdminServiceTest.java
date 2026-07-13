package com.nexuspms.governance.service;

import com.nexuspms.common.event.DomainEventPublisher;
import com.nexuspms.common.exception.GovernanceSafeguardException;
import com.nexuspms.governance.domain.Project;
import com.nexuspms.governance.domain.ProjectMethodology;
import com.nexuspms.governance.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/** PRD FR-8: delete requires archive-first -- the safeguard this test exists to lock in. */
@ExtendWith(MockitoExtension.class)
class ProjectAdminServiceTest {

    @Mock
    ProjectRepository projectRepository;
    @Mock
    AuditService auditService;
    @Mock
    DomainEventPublisher eventPublisher;

    private ProjectAdminService service() {
        return new ProjectAdminService(projectRepository, auditService, eventPublisher);
    }

    @Test
    void delete_withoutArchivingFirst_throwsGovernanceSafeguardException() {
        UUID projectId = UUID.randomUUID();
        Project project = new Project("NEX", "Nexus", "desc", ProjectMethodology.SCRUM, LocalDate.now(), null);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> service().delete(UUID.randomUUID(), projectId))
                .isInstanceOf(GovernanceSafeguardException.class);

        verify(auditService, never()).record(any(), any(), any(), any(), any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void delete_afterArchiving_succeeds() {
        UUID projectId = UUID.randomUUID();
        Project project = new Project("NEX", "Nexus", "desc", ProjectMethodology.SCRUM, LocalDate.now(), null);
        project.archive();
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        service().delete(UUID.randomUUID(), projectId);

        assertThat(project.getStatus().name()).isEqualTo("DELETED");
        verify(auditService).record(any(), eq("PROJECT_DELETED"), eq("PROJECT"), eq(projectId), any());
        verify(eventPublisher).publish(any());
    }

    @Test
    void create_withDuplicateProjectKey_throwsGovernanceSafeguardException() {
        when(projectRepository.existsByProjectKey("NEX")).thenReturn(true);

        assertThatThrownBy(() -> service().create(
                UUID.randomUUID(), "NEX", "Nexus", "desc", ProjectMethodology.SCRUM, LocalDate.now(), null))
                .isInstanceOf(GovernanceSafeguardException.class);
    }
}

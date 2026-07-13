package com.nexuspms.backlog.service;

import com.nexuspms.backlog.domain.Issue;
import com.nexuspms.backlog.domain.IssueType;
import com.nexuspms.backlog.event.IssueAssignedEvent;
import com.nexuspms.backlog.event.IssueCreatedEvent;
import com.nexuspms.backlog.event.IssueStatusChangedEvent;
import com.nexuspms.common.event.DomainEventPublisher;
import com.nexuspms.common.exception.ConcurrentModificationException;
import com.nexuspms.common.exception.ResourceNotFoundException;
import com.nexuspms.governance.domain.Project;
import com.nexuspms.governance.service.ProjectAdminService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * LLD S5 / S11.1: create/edit/delete issues across all five types, enforcing the
 * parent/child hierarchy rule and optimistic-lock conflict handling. RBAC and
 * workflow-transition legality are checked at the controller/aspect layer
 * (governance/security AuthorizationAspect) and by WorkflowTransitionValidator
 * respectively -- this service assumes both have already passed.
 *
 * New issues default to "To Do" (LLD/Database Design coupling note, see
 * IssueService.DEFAULT_INITIAL_STATUS javadoc) rather than looking up the
 * project's WorkflowDefinition, since Backlog & Issues does not depend on
 * Sprint & Board in the HLD S4 module dependency table -- only transitions
 * are validated against the workflow graph, not the initial creation status.
 */
@Service
public class IssueService {

    /**
     * Must match the name Sprint & Board's WorkflowService seeds as
     * is_initial=true on the default workflow (LLD S6.2) for every project.
     * If a project customizes its workflow's initial status name, issues
     * created here still default to this literal -- a real coupling this
     * comment exists to flag, not a live lookup.
     */
    public static final String DEFAULT_INITIAL_STATUS = "To Do";

    private static final BigDecimal RANK_INCREMENT = BigDecimal.valueOf(1024);

    private final com.nexuspms.backlog.repository.IssueRepository issueRepository;
    private final IssueKeyGenerator issueKeyGenerator;
    private final ProjectAdminService projectAdminService;
    private final DomainEventPublisher eventPublisher;

    public IssueService(com.nexuspms.backlog.repository.IssueRepository issueRepository,
                         IssueKeyGenerator issueKeyGenerator,
                         ProjectAdminService projectAdminService,
                         DomainEventPublisher eventPublisher) {
        this.issueRepository = issueRepository;
        this.issueKeyGenerator = issueKeyGenerator;
        this.projectAdminService = projectAdminService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Issue create(UUID projectId, IssueType issueType, UUID parentIssueId, String title, String description,
                         UUID reporterId, String priority, BigDecimal storyPoints, UUID assigneeId) {
        Project project = projectAdminService.get(projectId);

        BigDecimal nextRank = issueRepository.findMaxBacklogRank(projectId)
                .map(max -> max.add(RANK_INCREMENT))
                .orElse(RANK_INCREMENT);

        Issue issue = new Issue(projectId, issueKeyGenerator.next(projectId, project.getProjectKey()), issueType,
                parentIssueId, title, description, DEFAULT_INITIAL_STATUS, reporterId, priority, storyPoints, nextRank);
        issue.requireParentFor(issueType);
        if (assigneeId != null) {
            issue.applyEdit(null, null, null, null, assigneeId);
        }
        issueRepository.save(issue);
        eventPublisher.publish(new IssueCreatedEvent(issue.getId(), projectId));
        if (assigneeId != null) {
            eventPublisher.publish(new IssueAssignedEvent(issue.getId(), projectId, assigneeId));
        }
        return issue;
    }

    public Issue get(UUID issueId) {
        return issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue " + issueId + " not found."));
    }

    @Transactional
    public Issue edit(UUID issueId, long expectedVersion, String title, String description, String priority,
                       BigDecimal storyPoints, UUID assigneeId) {
        Issue issue = getWithVersionCheck(issueId, expectedVersion);
        boolean assigneeChanged = assigneeId != null && !assigneeId.equals(issue.getAssigneeId());
        issue.applyEdit(title, description, priority, storyPoints, assigneeId);
        if (assigneeChanged) {
            eventPublisher.publish(new IssueAssignedEvent(issue.getId(), issue.getProjectId(), assigneeId));
        }
        return issue;
    }

    @Transactional
    public void delete(UUID issueId) {
        Issue issue = get(issueId);
        issueRepository.delete(issue);
    }

    /**
     * LLD S6.2/S11.1: called only after both RBAC (TRANSITION_STATUS) and
     * WorkflowTransitionValidator have already approved the move -- this method
     * itself does not re-validate workflow legality, it just applies it and
     * publishes the event everything else (Reporting, Notifications, Real-Time,
     * Sprint & Board board state) reacts to.
     */
    @Transactional
    public Issue transition(UUID issueId, long expectedVersion, String newStatus) {
        Issue issue = getWithVersionCheck(issueId, expectedVersion);
        String fromStatus = issue.getStatus();
        issue.transitionTo(newStatus);
        eventPublisher.publish(new IssueStatusChangedEvent(issue.getId(), issue.getProjectId(), issue.getSprintId(), fromStatus, newStatus));
        return issue;
    }

    @Transactional
    public Issue moveToSprint(UUID issueId, long expectedVersion, UUID sprintId) {
        Issue issue = getWithVersionCheck(issueId, expectedVersion);
        if (sprintId == null) {
            issue.removeFromSprint();
        } else {
            issue.assignToSprint(sprintId);
        }
        return issue;
    }

    /** API Design S6 backlog reorder: recomputes backlog_rank without a full renumber (fractional indexing). */
    @Transactional
    public Issue reorder(UUID issueId, UUID afterIssueId, UUID beforeIssueId) {
        Issue issue = get(issueId);
        BigDecimal afterRank = afterIssueId != null ? get(afterIssueId).getBacklogRank() : null;
        BigDecimal beforeRank = beforeIssueId != null ? get(beforeIssueId).getBacklogRank() : null;

        BigDecimal newRank;
        if (afterRank != null && beforeRank != null) {
            newRank = afterRank.add(beforeRank).divide(BigDecimal.valueOf(2));
        } else if (afterRank != null) {
            newRank = afterRank.add(RANK_INCREMENT);
        } else if (beforeRank != null) {
            newRank = beforeRank.subtract(RANK_INCREMENT);
        } else {
            newRank = issueRepository.findMaxBacklogRank(issue.getProjectId()).map(m -> m.add(RANK_INCREMENT)).orElse(RANK_INCREMENT);
        }
        issue.moveBacklogRank(newRank);
        return issue;
    }

    /** LLD S11.2 sprint completion rollover: bulk re-parent, used both inline (small sprints) and by SprintRolloverJobHandler (large sprints). */
    @Transactional
    public void moveManyToSprint(List<UUID> issueIds, UUID sprintId) {
        for (UUID issueId : issueIds) {
            Issue issue = get(issueId);
            if (sprintId == null) {
                issue.removeFromSprint();
            } else {
                issue.assignToSprint(sprintId);
            }
        }
    }

    public List<Issue> listIncompleteInSprint(UUID sprintId, List<String> terminalStatuses) {
        return issueRepository.findBySprintIdAndStatusNotIn(sprintId, terminalStatuses);
    }

    public List<Issue> listBySprint(UUID sprintId) {
        return issueRepository.findBySprintId(sprintId);
    }

    public List<Issue> listByProject(UUID projectId) {
        return issueRepository.findByProjectId(projectId);
    }

    private Issue getWithVersionCheck(UUID issueId, long expectedVersion) {
        Issue issue = get(issueId);
        if (issue.getVersion() != expectedVersion) {
            throw new ConcurrentModificationException(
                    "Issue " + issue.getIssueKey() + " has changed since it was last read.", expectedVersion, issue.getVersion());
        }
        return issue;
    }
}

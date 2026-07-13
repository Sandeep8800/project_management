package com.nexuspms.backlog.repository;

import com.nexuspms.backlog.domain.Issue;
import com.nexuspms.backlog.domain.IssueType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IssueRepository extends JpaRepository<Issue, UUID> {

    Optional<Issue> findByIssueKey(String issueKey);

    long countByProjectId(UUID projectId);

    List<Issue> findByParentIssueId(UUID parentIssueId);

    List<Issue> findBySprintId(UUID sprintId);

    List<Issue> findByProjectId(UUID projectId);

    List<Issue> findBySprintIdAndStatusNotIn(UUID sprintId, List<String> statuses);

    /**
     * API Design S2.2: keyset pagination on backlog_rank -- avoids the OFFSET-scan
     * cost at 10k+ issues/project (Database Design S11). "q" free-text search is
     * a separate path (IssueSearchService, native query against search_vector)
     * not folded in here; label filtering is a known gap for this pass -- see
     * IssueController TODO.
     */
    @Query("""
            select i from Issue i
            where i.projectId = :projectId
              and (:status is null or i.status = :status)
              and (:assigneeId is null or i.assigneeId = :assigneeId)
              and (:sprintId is null or i.sprintId = :sprintId)
              and (:onlyBacklog = false or i.sprintId is null)
              and (:issueType is null or i.issueType = :issueType)
              and (:afterRank is null or i.backlogRank > :afterRank)
            order by i.backlogRank asc
            """)
    List<Issue> keysetSearch(@Param("projectId") UUID projectId,
                              @Param("status") String status,
                              @Param("assigneeId") UUID assigneeId,
                              @Param("sprintId") UUID sprintId,
                              @Param("onlyBacklog") boolean onlyBacklog,
                              @Param("issueType") IssueType issueType,
                              @Param("afterRank") BigDecimal afterRank,
                              org.springframework.data.domain.Pageable limit);

    @Query("select max(i.backlogRank) from Issue i where i.projectId = :projectId")
    Optional<BigDecimal> findMaxBacklogRank(@Param("projectId") UUID projectId);

    /**
     * HLD S5 search seam: Postgres full-text search via the generated
     * search_vector column, behind this one query method -- IssueSearchService
     * is the only caller, so swapping to an external engine later touches this
     * method's implementation only, not every list-issues call site.
     */
    @Query(value = """
            select * from issues
            where project_id = :projectId
              and search_vector @@ plainto_tsquery('english', :q)
            order by ts_rank(search_vector, plainto_tsquery('english', :q)) desc
            limit :limit
            """, nativeQuery = true)
    List<Issue> fullTextSearch(@Param("projectId") UUID projectId, @Param("q") String q, @Param("limit") int limit);
}

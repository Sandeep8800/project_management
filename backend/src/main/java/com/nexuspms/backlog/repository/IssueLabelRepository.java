package com.nexuspms.backlog.repository;

import com.nexuspms.backlog.domain.IssueLabel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IssueLabelRepository extends JpaRepository<IssueLabel, IssueLabel.Key> {

    List<IssueLabel> findByIssueId(UUID issueId);

    void deleteByIssueIdAndLabelId(UUID issueId, UUID labelId);
}

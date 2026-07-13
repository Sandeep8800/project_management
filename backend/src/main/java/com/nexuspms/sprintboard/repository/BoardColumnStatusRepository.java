package com.nexuspms.sprintboard.repository;

import com.nexuspms.sprintboard.domain.BoardColumnStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BoardColumnStatusRepository extends JpaRepository<BoardColumnStatus, BoardColumnStatus.Key> {

    List<BoardColumnStatus> findByBoardColumnId(UUID boardColumnId);

    List<BoardColumnStatus> findByWorkflowStatusId(UUID workflowStatusId);

    void deleteByBoardColumnId(UUID boardColumnId);
}

package com.nexuspms.sprintboard.repository;

import com.nexuspms.sprintboard.domain.BoardColumn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BoardColumnRepository extends JpaRepository<BoardColumn, UUID> {

    List<BoardColumn> findByBoardIdOrderByDisplayOrder(UUID boardId);
}

package com.nexuspms.sprintboard.repository;

import com.nexuspms.sprintboard.domain.Board;
import com.nexuspms.sprintboard.domain.BoardType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BoardRepository extends JpaRepository<Board, UUID> {

    Optional<Board> findByProjectIdAndBoardType(UUID projectId, BoardType boardType);

    List<Board> findByProjectId(UUID projectId);
}

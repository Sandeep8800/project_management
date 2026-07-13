package com.nexuspms.backlog.repository;

import com.nexuspms.backlog.domain.Label;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LabelRepository extends JpaRepository<Label, UUID> {

    List<Label> findByProjectId(UUID projectId);

    Optional<Label> findByProjectIdAndName(UUID projectId, String name);
}

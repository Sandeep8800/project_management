package com.nexuspms.backlog.domain;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "labels")
public class Label {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(nullable = false)
    private String name;

    protected Label() {
        // JPA
    }

    public Label(UUID projectId, String name) {
        this.projectId = projectId;
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public String getName() {
        return name;
    }
}

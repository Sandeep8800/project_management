package com.nexuspms.sprintboard.api.dto;

public record ConfigureColumnRequest(String name, Integer displayOrder, Integer wipLimit) {
}

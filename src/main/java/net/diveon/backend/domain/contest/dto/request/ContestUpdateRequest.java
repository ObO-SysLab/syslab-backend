package net.diveon.backend.domain.contest.dto.request;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;

public class ContestUpdateRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String description;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
}

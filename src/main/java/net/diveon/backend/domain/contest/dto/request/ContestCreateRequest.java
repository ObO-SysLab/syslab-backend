package net.diveon.backend.domain.contest.dto.request;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import net.diveon.backend.domain.contest.entity.Contest;

public class ContestCreateRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String description;

    @NotNull
    private Contest.ContestType contestType;

    @NotNull
    private Contest.ParticipationType participationType;

    @NotNull
    private Contest.Visibility visibility;

    @NotNull
    private LocalDateTime startTime;

    @NotNull
    private LocalDateTime endTime;

    @NotBlank
    private String rules;

    private String prizeDescription;
    private List<String> tags;

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Contest.ContestType getContestType() { return contestType; }
    public Contest.ParticipationType getParticipationType() { return participationType; }
    public Contest.Visibility getVisibility() { return visibility; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public String getRules() { return rules; }
    public String getPrizeDescription() { return prizeDescription; }
    public List<String> getTags() { return tags; }
}

package net.diveon.backend.domain.user.dto;

import net.diveon.backend.domain.contest.entity.ContestParticipant;

import java.time.LocalDateTime;

public class MyContestItemResponse {

    private final Long contestId;
    private final String title;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final String status;
    private final String role;

    public MyContestItemResponse(Long contestId, String title, LocalDateTime startTime, LocalDateTime endTime, String status, String role) {
        this.contestId = contestId;
        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.role = role;
    }

    public static MyContestItemResponse of(ContestParticipant cp) {
        return new MyContestItemResponse(
            cp.getContest().getId(),
            cp.getContest().getTitle(),
            cp.getContest().getStartTime(),
            cp.getContest().getEndTime(),
            cp.getContest().getStatus().name(),
            cp.getRole().name()
        );
    }

    public Long getContestId() { return contestId; }
    public String getTitle() { return title; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public String getStatus() { return status; }
    public String getRole() { return role; }
}

package net.diveon.backend.domain.contest.dto.response;

public class ContestCreateResponse {

    private Long contestId;
    private String title;

    public ContestCreateResponse(Long contestId, String title) {
        this.contestId = contestId;
        this.title = title;
    }

    public Long getContestId() { return contestId; }
    public String getTitle() { return title; }
}

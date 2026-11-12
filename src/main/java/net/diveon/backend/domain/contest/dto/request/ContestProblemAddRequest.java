package net.diveon.backend.domain.contest.dto.request;

public class ContestProblemAddRequest {

    private Long problemId;
    private Integer points;

    public Long getProblemId() {
        return problemId;
    }

    public Integer getPoints() {
        return points;
    }
}

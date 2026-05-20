package net.diveon.backend.domain.contest.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ContestSubmitImmediateResponse {

    @JsonProperty("isCorrect")
    private Boolean isCorrect;

    @JsonProperty("score")
    private Integer score;

    public ContestSubmitImmediateResponse() {}

    public ContestSubmitImmediateResponse(Boolean isCorrect, Integer score) {
        this.isCorrect = isCorrect;
        this.score = score;
    }

    public Boolean getIsCorrect() { return isCorrect; }
    public Integer getScore() { return score; }
}
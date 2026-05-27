package net.diveon.backend.domain.contest.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ContestSubmitImmediateResponse {

    @JsonProperty("submissionId")
    private Long submissionId;

    @JsonProperty("submissionStatus")
    private String submissionStatus;

    @JsonProperty("isCorrect")
    private Boolean isCorrect;

    @JsonProperty("score")
    private Integer score;

    @JsonProperty("rank")
    private Integer rank;

    public ContestSubmitImmediateResponse() {}

    public ContestSubmitImmediateResponse(Long submissionId, Boolean isCorrect, Integer score, Integer rank) {
        this.submissionId = submissionId;
        this.submissionStatus = "COMPLETED";
        this.isCorrect = isCorrect;
        this.score = score;
        this.rank = rank;
    }

    public Long getSubmissionId() { return submissionId; }
    public String getSubmissionStatus() { return submissionStatus; }
    public Boolean getIsCorrect() { return isCorrect; }
    public Integer getScore() { return score; }
    public Integer getRank() { return rank; }
}

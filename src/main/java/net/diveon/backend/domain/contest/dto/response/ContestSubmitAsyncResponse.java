package net.diveon.backend.domain.contest.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ContestSubmitAsyncResponse {

    @JsonProperty("submissionId")
    private Long submissionId;

    @JsonProperty("submissionStatus")
    private String submissionStatus;
    private Long solveSubmissionId;

    public Long getSolveSubmissionId() { return solveSubmissionId; }

    public ContestSubmitAsyncResponse() {}

    public ContestSubmitAsyncResponse(Long submissionId, String submissionStatus, Long solveSubmissionId) {
        this.solveSubmissionId = solveSubmissionId;
        this.submissionId = submissionId;
        this.submissionStatus = submissionStatus;
    }

    public Long getSubmissionId() { return submissionId; }
    public String getSubmissionStatus() { return submissionStatus; }
}

package net.diveon.backend.domain.contest.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ContestSubmitAsyncResponse {

    @JsonProperty("submissionId")
    private Long submissionId;

    @JsonProperty("status")
    private String status;

    public ContestSubmitAsyncResponse() {}

    public ContestSubmitAsyncResponse(Long submissionId, String status) {
        this.submissionId = submissionId;
        this.status = status;
    }

    public Long getSubmissionId() { return submissionId; }
    public String getStatus() { return status; }
}
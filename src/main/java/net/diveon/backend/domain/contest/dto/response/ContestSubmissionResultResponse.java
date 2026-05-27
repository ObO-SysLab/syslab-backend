package net.diveon.backend.domain.contest.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ContestSubmissionResultResponse {

    @JsonProperty("submissionId")
    private Long submissionId;

    @JsonProperty("language")
    private String language;

    @JsonProperty("code")
    private String code;

    public ContestSubmissionResultResponse() {}

    public ContestSubmissionResultResponse(Long submissionId, String language, String code) {
        this.submissionId = submissionId;
        this.language = language;
        this.code = code;
    }

    public Long getSubmissionId() { return submissionId; }
    public String getLanguage() { return language; }
    public String getCode() { return code; }
}

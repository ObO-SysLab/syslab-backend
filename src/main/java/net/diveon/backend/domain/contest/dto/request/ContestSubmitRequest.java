package net.diveon.backend.domain.contest.dto.request;

import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ContestSubmitRequest {

    public enum SubmissionType {
        OBJECTIVE, PRACTICE, CODING
    }

    @NotNull
    @JsonProperty("submissionType")
    private SubmissionType submissionType;

    @JsonProperty("answer")
    private String answer;

    @JsonProperty("code")
    private String code;

    @JsonProperty("language")
    private String language;

    public ContestSubmitRequest() {}

    public SubmissionType getSubmissionType() { return submissionType; }
    public String getAnswer() { return answer; }
    public String getCode() { return code; }
    public String getLanguage() { return language; }
}
package net.diveon.backend.domain.grade.dto.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SubmissionResultPracticeResponse implements SubmissionResultResponse {

    @JsonProperty("submissionId")
    private final long submissionId;

    @JsonProperty("probId")
    private final long probId;

    @JsonProperty("probType")
    private final String probType = "practice";

    @JsonProperty("isCorrect")
    private final boolean isCorrect;

    @JsonProperty("submittedAt")
    private final LocalDateTime submittedAt;

    @JsonProperty("judgedAt")
    private final LocalDateTime judgedAt;

    public SubmissionResultPracticeResponse(
        long submissionId,
        long probId,
        boolean isCorrect,
        LocalDateTime submittedAt,
        LocalDateTime judgedAt
    ) {
        this.submissionId = submissionId;
        this.probId = probId;
        this.isCorrect = isCorrect;
        this.submittedAt = submittedAt;
        this.judgedAt = judgedAt;
    }

    @Override
    public String getProbType() { return probType; }
    public long getSubmissionId() { return submissionId; }
    public long getProbId() { return probId; }
    public boolean isCorrect() { return isCorrect; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public LocalDateTime getJudgedAt() { return judgedAt; }
}

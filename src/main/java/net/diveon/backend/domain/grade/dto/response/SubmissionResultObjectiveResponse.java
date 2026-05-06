package net.diveon.backend.domain.grade.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SubmissionResultObjectiveResponse implements SubmissionResultResponse {

    @JsonProperty("submissionId")
    private final long submissionId;

    @JsonProperty("probId")
    private final long probId;

    @JsonProperty("probType")
    private final String probType = "objective";

    @JsonProperty("isCorrect")
    private final boolean isCorrect;

    @JsonProperty("selectedAnswers")
    private final List<Integer> selectedAnswers;

    @JsonProperty("submittedAt")
    private final LocalDateTime submittedAt;

    @JsonProperty("judgedAt")
    private final LocalDateTime judgedAt;

    public SubmissionResultObjectiveResponse(
        long submissionId,
        long probId,
        boolean isCorrect,
        List<Integer> selectedAnswers,
        LocalDateTime submittedAt,
        LocalDateTime judgedAt
    ) {
        this.submissionId = submissionId;
        this.probId = probId;
        this.isCorrect = isCorrect;
        this.selectedAnswers = selectedAnswers;
        this.submittedAt = submittedAt;
        this.judgedAt = judgedAt;
    }

    @Override
    public String getProbType() { return probType; }
    public long getSubmissionId() { return submissionId; }
    public long getProbId() { return probId; }
    @JsonIgnore
    public boolean isCorrect() { return isCorrect; }
    public List<Integer> getSelectedAnswers() { return selectedAnswers; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public LocalDateTime getJudgedAt() { return judgedAt; }
}

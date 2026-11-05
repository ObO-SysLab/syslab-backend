package net.diveon.backend.domain.grade.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SubmissionStatusResponse {

    @JsonProperty("submissionId")
    private final String submissionId;

    @JsonProperty("probId")
    private final long probId;

    @JsonProperty("probType")
    private final String problemType;

    @JsonProperty("submissionStatus")
    private final String submissionStatus;

    @JsonProperty("progress")
    private final int progress;

    private final String resultStatus;

    public SubmissionStatusResponse(
        String submissionId,
        long probId,
        String problemType,
        String submissionStatus,
        int progress,
        String resultStatus
    ) {
        this.submissionId = submissionId;
        this.probId = probId;
        this.problemType = problemType;
        this.submissionStatus = submissionStatus;
        this.progress = progress;
        this.resultStatus = resultStatus;
    }

    public String getSubmissionId() {
        return submissionId;
    }

    public long getProbId() {
        return probId;
    }

    public String getProblemType() {
        return problemType;
    }

    public String getSubmissionStatus() {
        return submissionStatus;
    }

    public String getResultStatus() { return resultStatus; }

    public int getProgress() {
        return progress;
    }
}

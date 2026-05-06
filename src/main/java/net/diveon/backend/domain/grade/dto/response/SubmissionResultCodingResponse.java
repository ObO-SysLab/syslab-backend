package net.diveon.backend.domain.grade.dto.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SubmissionResultCodingResponse implements SubmissionResultResponse {

    @JsonProperty("submissionId")
    private final long submissionId;

    @JsonProperty("probId")
    private final long probId;

    @JsonProperty("probType")
    private final String probType = "coding";

    @JsonProperty("language")
    private final String language;

    @JsonProperty("isCorrect")
    private final boolean isCorrect;

    @JsonProperty("score")
    private final short score;

    @JsonProperty("runtime")
    private final int runtime;

    @JsonProperty("memoryUsage")
    private final int memoryUsage;

    @JsonProperty("codeSize")
    private final int codeSize;

    @JsonProperty("submittedAt")
    private final LocalDateTime submittedAt;

    @JsonProperty("judgedAt")
    private final LocalDateTime judgedAt;

    @JsonProperty("code")
    private final String code;

    public SubmissionResultCodingResponse(
        long submissionId,
        long probId,
        String language,
        boolean isCorrect,
        short score,
        int runtime,
        int memoryUsage,
        int codeSize,
        LocalDateTime submittedAt,
        LocalDateTime judgedAt,
        String code
    ) {
        this.submissionId = submissionId;
        this.probId = probId;
        this.language = language;
        this.isCorrect = isCorrect;
        this.score = score;
        this.runtime = runtime;
        this.memoryUsage = memoryUsage;
        this.codeSize = codeSize;
        this.submittedAt = submittedAt;
        this.judgedAt = judgedAt;
        this.code = code;
    }

    @Override
    public String getProbType() { return probType; }
    public long getSubmissionId() { return submissionId; }
    public long getProbId() { return probId; }
    public String getLanguage() { return language; }
    @JsonIgnore
    public boolean isCorrect() { return isCorrect; }
    public short getScore() { return score; }
    public int getRuntime() { return runtime; }
    public int getMemoryUsage() { return memoryUsage; }
    public int getCodeSize() { return codeSize; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public LocalDateTime getJudgedAt() { return judgedAt; }
    public String getCode() { return code; }
}

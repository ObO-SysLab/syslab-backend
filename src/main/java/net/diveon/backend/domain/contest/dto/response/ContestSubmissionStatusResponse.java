package net.diveon.backend.domain.contest.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContestSubmissionStatusResponse {

    @JsonProperty("submissionId")
    private Long submissionId;

    @JsonProperty("submissionStatus")
    private String submissionStatus;

    @JsonProperty("progress")
    private Integer progress;

    @JsonProperty("isCorrect")
    private Boolean isCorrect;

    @JsonProperty("score")
    private Integer score;

    @JsonProperty("rank")
    private Integer rank;

    public ContestSubmissionStatusResponse() {}

    public static ContestSubmissionStatusResponse pending(Long submissionId, String status, int progress) {
        ContestSubmissionStatusResponse r = new ContestSubmissionStatusResponse();
        r.submissionId = submissionId;
        r.submissionStatus = status;
        r.progress = progress;
        return r;
    }

    public static ContestSubmissionStatusResponse completed(Long submissionId, boolean isCorrect, int score, int rank) {
        ContestSubmissionStatusResponse r = new ContestSubmissionStatusResponse();
        r.submissionId = submissionId;
        r.submissionStatus = "COMPLETED";
        r.progress = 100;
        r.isCorrect = isCorrect;
        r.score = score;
        r.rank = rank;
        return r;
    }

    public Long getSubmissionId() { return submissionId; }
    public String getSubmissionStatus() { return submissionStatus; }
    public Integer getProgress() { return progress; }
    public Boolean getIsCorrect() { return isCorrect; }
    public Integer getScore() { return score; }
    public Integer getRank() { return rank; }
}

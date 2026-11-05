package net.diveon.backend.domain.grade.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.diveon.backend.domain.grade.dto.response.interfaces.GradeBoardResponse;

import java.util.List;

public class GradeBoardObjectiveResponse implements GradeBoardResponse {

    @JsonProperty("probId")
    private Long probId;

    private Long total;

    private List<SubmissionItem> submissions;

    public GradeBoardObjectiveResponse(Long probId, Long total, List<SubmissionItem> submissions) {
        this.probId = probId;
        this.total = total;
        this.submissions = submissions;
    }

    public Long getProbId() { return probId; }
    public Long getTotal() { return total; }
    public List<SubmissionItem> getSubmissions() { return submissions; }

    public static class SubmissionItem {

        @JsonProperty("submissionId")
        private Long submissionId;

        private String nickname;
        private Integer userTier;
        private boolean hasObo;

        @JsonProperty("isCorrect")
        private Boolean isCorrect;
        private String resultStatus;

        @JsonProperty("submittedAt")
        private String submittedAt;

        @JsonProperty("judgedAt")
        private String judgedAt;

        public SubmissionItem(Long submissionId, String nickname, Boolean isCorrect, String resultStatus,
                              String submittedAt, String judgedAt, Integer userTier, boolean hasObo) {
            this.submissionId = submissionId;
            this.nickname = nickname;
            this.userTier = userTier;
            this.hasObo = hasObo;
            this.isCorrect = isCorrect;
            this.resultStatus = resultStatus;
            this.submittedAt = submittedAt;
            this.judgedAt = judgedAt;
        }

        public Long getSubmissionId() { return submissionId; }
        public String getNickname() { return nickname; }
        public Integer getUserTier() { return userTier; }
        public boolean getHasObo() { return hasObo; }
        public Boolean getIsCorrect() { return isCorrect; }
        public String getResultStatus() { return resultStatus; }
        public String getSubmittedAt() { return submittedAt; }
        public String getJudgedAt() { return judgedAt; }
    }
}

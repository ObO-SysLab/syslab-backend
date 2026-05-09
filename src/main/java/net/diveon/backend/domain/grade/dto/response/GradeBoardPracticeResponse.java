package net.diveon.backend.domain.grade.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.diveon.backend.domain.grade.dto.response.interfaces.GradeBoardResponse;

import java.util.List;

public class GradeBoardPracticeResponse implements GradeBoardResponse {

    @JsonProperty("probId")
    private Long probId;

    private Long total;

    private List<SubmissionItem> submissions;

    public GradeBoardPracticeResponse(Long probId, Long total, List<SubmissionItem> submissions) {
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

        @JsonProperty("isCorrect")
        private Boolean isCorrect;

        @JsonProperty("submittedAt")
        private String submittedAt;

        @JsonProperty("judgedAt")
        private String judgedAt;

        public SubmissionItem(Long submissionId, String nickname, Boolean isCorrect,
                              String submittedAt, String judgedAt) {
            this.submissionId = submissionId;
            this.nickname = nickname;
            this.isCorrect = isCorrect;
            this.submittedAt = submittedAt;
            this.judgedAt = judgedAt;
        }

        public Long getSubmissionId() { return submissionId; }
        public String getNickname() { return nickname; }
        public Boolean getIsCorrect() { return isCorrect; }
        public String getSubmittedAt() { return submittedAt; }
        public String getJudgedAt() { return judgedAt; }
    }
}

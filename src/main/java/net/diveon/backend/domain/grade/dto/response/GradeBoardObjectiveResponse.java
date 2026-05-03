package net.diveon.backend.domain.grade.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.diveon.backend.domain.grade.dto.response.interfaces.GradeBoardResponse;

import java.util.List;

public class GradeBoardObjectiveResponse implements GradeBoardResponse {

    @JsonProperty("prob_id")
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

        @JsonProperty("submission_id")
        private Long submissionId;

        private String nickname;

        @JsonProperty("is_correct")
        private Boolean isCorrect;

        private Short score;

        @JsonProperty("submitted_at")
        private String submittedAt;

        @JsonProperty("judged_at")
        private String judgedAt;

        public SubmissionItem(Long submissionId, String nickname, Boolean isCorrect,
                              Short score, String submittedAt, String judgedAt) {
            this.submissionId = submissionId;
            this.nickname = nickname;
            this.isCorrect = isCorrect;
            this.score = score;
            this.submittedAt = submittedAt;
            this.judgedAt = judgedAt;
        }

        public Long getSubmissionId() { return submissionId; }
        public String getNickname() { return nickname; }
        public Boolean getIsCorrect() { return isCorrect; }
        public Short getScore() { return score; }
        public String getSubmittedAt() { return submittedAt; }
        public String getJudgedAt() { return judgedAt; }
    }
}

package net.diveon.backend.domain.grade.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.diveon.backend.domain.grade.dto.response.interfaces.GradeBoardResponse;

import java.util.List;

public class GradeBoardCodingResponse implements GradeBoardResponse {

    @JsonProperty("probId")
    private Long probId;

    private Long total;

    private List<SubmissionItem> submissions;

    public GradeBoardCodingResponse(Long probId, Long total, List<SubmissionItem> submissions) {
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

        private String language;

        @JsonProperty("isCorrect")
        private Boolean isCorrect;

        private Short score;

        private Integer runtime;

        @JsonProperty("memoryUsage")
        private Integer memoryUsage;

        @JsonProperty("codeSize")
        private Integer codeSize;

        @JsonProperty("submittedAt")
        private String submittedAt;

        @JsonProperty("judgedAt")
        private String judgedAt;

        public SubmissionItem(Long submissionId, String nickname, String language, Boolean isCorrect,
                              Short score, Integer runtime, Integer memoryUsage, Integer codeSize,
                              String submittedAt, String judgedAt) {
            this.submissionId = submissionId;
            this.nickname = nickname;
            this.language = language;
            this.isCorrect = isCorrect;
            this.score = score;
            this.runtime = runtime;
            this.memoryUsage = memoryUsage;
            this.codeSize = codeSize;
            this.submittedAt = submittedAt;
            this.judgedAt = judgedAt;
        }

        public Long getSubmissionId() { return submissionId; }
        public String getNickname() { return nickname; }
        public String getLanguage() { return language; }
        public Boolean getIsCorrect() { return isCorrect; }
        public Short getScore() { return score; }
        public Integer getRuntime() { return runtime; }
        public Integer getMemoryUsage() { return memoryUsage; }
        public Integer getCodeSize() { return codeSize; }
        public String getSubmittedAt() { return submittedAt; }
        public String getJudgedAt() { return judgedAt; }
    }
}

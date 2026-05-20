package net.diveon.backend.domain.grade.dto.message;

public class CodingGradeMessage {
    private String submissionId;
    private String s3Key;
    private String language;
    private String problemId;
    private Integer timeLimit;
    private Integer memoryLimit;
    private Integer testcaseCount;
    private ContestContext contestContext;

    public CodingGradeMessage() {}

    public CodingGradeMessage(
        String submissionId,
        String s3Key,
        String language,
        String problemId,
        Integer timeLimit,
        Integer memoryLimit,
        Integer testcaseCount
    ) {
        this.submissionId = submissionId;
        this.s3Key = s3Key;
        this.language = language;
        this.problemId = problemId;
        this.timeLimit = timeLimit;
        this.memoryLimit = memoryLimit;
        this.testcaseCount = testcaseCount;
    }

    public String getSubmissionId() { return submissionId; }
    public String getS3Key() { return s3Key; }
    public String getLanguage() { return language; }
    public String getProblemId() { return problemId; }
    public Integer getTimeLimit() { return timeLimit; }
    public Integer getMemoryLimit() { return memoryLimit; }
    public Integer getTestcaseCount() { return testcaseCount; }
    public ContestContext getContestContext() { return contestContext; }
    public void setContestContext(ContestContext contestContext) { this.contestContext = contestContext; }

    public static class ContestContext {
        private Long contestId;
        private Long contestProblemId;
        private Integer points;

        public ContestContext() {}

        public ContestContext(Long contestId, Long contestProblemId, Integer points) {
            this.contestId = contestId;
            this.contestProblemId = contestProblemId;
            this.points = points;
        }

        public Long getContestId() { return contestId; }
        public Long getContestProblemId() { return contestProblemId; }
        public Integer getPoints() { return points; }
    }
}

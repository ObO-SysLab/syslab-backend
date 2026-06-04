package net.diveon.backend.domain.problem.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.diveon.backend.domain.problem.dto.response.interfaces.ProblemDetailResponse;
import net.diveon.backend.domain.problem.entity.Problem;
import net.diveon.backend.domain.problem.entity.ProblemCoding;
import net.diveon.backend.domain.problem.others.ForDtoTestCase;

import java.util.List;

/**
 * 코딩형 문제 상세 조회 응답의 data 영역을 위한 DTO
 *
 * <pre>
 * {
 *   "status": 200,
 *   "message": "문제 상세 조회에 성공하였습니다.",
 *   "data": {
 *     "probId": 99,
 *     "author": "박단용",
 *     "type": "coding",
 *     "title": "프로세스 우선순위 정렬",
 *     "description": "N개의 프로세스가 주어질 때, 우선순위 스케줄링 순서대로 프로세스 ID를 출력하시오.",
 *     "category": "scheduling",
 *     "difficulty": "medium",
 *     "visibility": "public",
 *     "summary": "프로세스 우선순위 정렬 문제",
 *     "constraints": {
 *       "timeLimitMs": 1000,
 *       "memoryLimitMb": 256,
 *       "allowedLanguages": ["python", "java", "c", "cpp"]
 *     },
 *     "inputDescription": "첫째 줄에 N(1 ≤ N ≤ 100), 이후 N줄에 걸쳐 프로세스 ID와 우선순위가 주어진다.",
 *     "outputDescription": "우선순위가 높은 순으로 프로세스 ID를 한 줄씩 출력한다.",
 *     "testcases": [
 *       {
 *         "index": 1,
 *         "input": "3\n1 5\n2 3\n3 8",
 *         "output": "3\n1\n2",
 *         "isSample": true
 *       }
 *     ],
 *     "fileUrl": null,
 *     "solvedCount": 45,
 *     "submittedCount": 78,
 *     "createdAt": "2025-01-20T08:30:00Z",
 *     "updatedAt": "2025-01-25T14:15:00Z"
 *   }
 * }
 * </pre>
 */
public class ProblemDetailCodingResponse implements ProblemDetailResponse {

    @JsonProperty("probId")
    private Long probId;

    private String author;
    private String type;
    private String title;
    private String description;
    private String category;
    private String difficulty;
    private String visibility;
    private String summary;
    private Constraints constraints;

    @JsonProperty("inputDescription")
    private String inputDescription;

    @JsonProperty("outputDescription")
    private String outputDescription;

    private List<ForDtoTestCase> testcases;

    @JsonProperty("fileUrl")
    private String fileUrl;

    @JsonProperty("solvedCount")
    private Integer solvedCount;

    @JsonProperty("submittedCount")
    private Integer submittedCount;

    @JsonProperty("createdAt")
    private String createdAt;

    @JsonProperty("updatedAt")
    private String updatedAt;

    public ProblemDetailCodingResponse() {
    }

    public ProblemDetailCodingResponse(
        Long probId,
        String author,
        String type,
        String title,
        String description,
        String category,
        String difficulty,
        String visibility,
        String summary,
        Constraints constraints,
        String inputDescription,
        String outputDescription,
        List<ForDtoTestCase> testcases,
        String fileUrl,
        Integer solvedCount,
        Integer submittedCount,
        String createdAt,
        String updatedAt
    ) {
        this.probId = probId;
        this.author = author;
        this.type = type;
        this.title = title;
        this.description = description;
        this.category = category;
        this.difficulty = difficulty;
        this.visibility = visibility;
        this.summary = summary;
        this.constraints = constraints;
        this.inputDescription = inputDescription;
        this.outputDescription = outputDescription;
        this.testcases = testcases;
        this.fileUrl = fileUrl;
        this.solvedCount = solvedCount;
        this.submittedCount = submittedCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ProblemDetailCodingResponse of(
        Problem problem,
        ProblemCoding problemCoding
    ) {
        Constraints constraints = new Constraints(
            problemCoding.getTimeLimitMs(),
            problemCoding.getMemoryLimitMb(),
            problemCoding.getAllowedLanguages()
        );

        return new ProblemDetailCodingResponse(
            problem.getId(),
            resolveAuthorName(problem),
            problem.getType(),
            problem.getTitle(),
            problemCoding.getDescription(),
            problem.getCategory(),
            problem.getDifficulty(),
            problem.getVisibility(),
            problemCoding.getSummary(),
            constraints,
            problemCoding.getInputDescription(),
            problemCoding.getOutputDescription(),
            problemCoding.getTestcases(),
            problemCoding.getFileUrl(),
            problem.getSolvedCount(),
            problem.getSubmittedCount(),
            problem.getCreatedAt().toString(),
            problem.getUpatedAt().toString()
        );
    }

    private static String resolveAuthorName(Problem problem) {
        if ("ExitedUser".equals(problem.getAuthor().getRealName())) {
            return "탈퇴한유저";
        }
        return problem.getAuthor().getNickname();
    }

    // Getters
    public Long getProbId() { return probId; }
    public String getAuthor() { return author; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public String getDifficulty() { return difficulty; }
    public String getVisibility() { return visibility; }
    public String getSummary() { return summary; }
    public Constraints getConstraints() { return constraints; }
    public String getInputDescription() { return inputDescription; }
    public String getOutputDescription() { return outputDescription; }
    public List<ForDtoTestCase> getTestcases() { return testcases; }
    public String getFileUrl() { return fileUrl; }
    public Integer getSolvedCount() { return solvedCount; }
    public Integer getSubmittedCount() { return submittedCount; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }

    public static class Constraints {
        @JsonProperty("timeLimitMs")
        private Integer timeLimitMs;

        @JsonProperty("memoryLimitMb")
        private Integer memoryLimitMb;

        @JsonProperty("allowedLanguages")
        private List<String> allowedLanguages;

        public Constraints() {
        }

        public Constraints(Integer timeLimitMs, Integer memoryLimitMb, List<String> allowedLanguages) {
            this.timeLimitMs = timeLimitMs;
            this.memoryLimitMb = memoryLimitMb;
            this.allowedLanguages = allowedLanguages;
        }

        public Integer getTimeLimitMs() { return timeLimitMs; }
        public Integer getMemoryLimitMb() { return memoryLimitMb; }
        public List<String> getAllowedLanguages() { return allowedLanguages; }
    }
}

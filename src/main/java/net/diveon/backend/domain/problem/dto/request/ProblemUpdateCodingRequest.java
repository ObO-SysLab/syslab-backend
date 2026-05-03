package net.diveon.backend.domain.problem.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.diveon.backend.domain.problem.others.ForDtoTestCase;

import java.util.List;

/**
 * 다음과 같은 형태의 PATCH request 양식을 만족하기 위한 DTO
 * <pre>
 * {
 *   "title": "프로세스 우선순위 정렬 심화",
 *   "description": "N개의 프로세스가 주어질 때...",
 *   "category": "scheduling",
 *   "difficulty": "hard",
 *   "visibility": "public",
 *   "summary": "프로세스 우선순위 정렬 심화 문제",
 *   "inputDescription": "첫째 줄에 N...",
 *   "outputDescription": "우선순위가 높은 순으로...",
 *   "constraints": {
 *     "timeLimitMs": 2000,
 *     "memoryLimitMb": 512,
 *     "allowedLanguages": ["python", "java", "c", "cpp", "go"]
 *   },
 *   "testcases": [
 *     {
 *       "index": 1,
 *       "input": "3\n1 5\n2 3\n3 8",
 *       "output": "3\n1\n2",
 *       "isSample": true
 *     }
 *   ],
 *   "fileUrl": null,
 *   "obo": {
 *     "enabled": true,
 *     "initialImageUrl": "https://cdn.dk-world.com/problems/p99_init.png"
 *   }
 * }
 * </pre>
 */
public class ProblemUpdateCodingRequest {

    private String title;
    private String summary;
    private String description;
    private String category;
    private String difficulty;
    private String visibility;
    private Constraints constraints;

    @JsonProperty("inputDescription")
    private String inputDescription;

    @JsonProperty("outputDescription")
    private String outputDescription;

    private List<ForDtoTestCase> testcases;

    @JsonProperty("fileUrl")
    private String fileUrl;

    private Obo obo;

    public ProblemUpdateCodingRequest() {
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }

    public Constraints getConstraints() { return constraints; }
    public void setConstraints(Constraints constraints) { this.constraints = constraints; }

    public String getInputDescription() { return inputDescription; }
    public void setInputDescription(String inputDescription) { this.inputDescription = inputDescription; }

    public String getOutputDescription() { return outputDescription; }
    public void setOutputDescription(String outputDescription) { this.outputDescription = outputDescription; }

    public List<ForDtoTestCase> getTestcases() { return testcases; }
    public void setTestcases(List<ForDtoTestCase> testcases) { this.testcases = testcases; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public Obo getObo() { return obo; }
    public void setObo(Obo obo) { this.obo = obo; }

    public static class Constraints {
        @JsonProperty("timeLimitMs")
        private Integer timeLimitMs;

        @JsonProperty("memoryLimitMb")
        private Integer memoryLimitMb;

        @JsonProperty("allowedLanguages")
        private List<String> allowedLanguages;

        public Constraints() {
        }

        public Integer getTimeLimitMs() { return timeLimitMs; }
        public void setTimeLimitMs(Integer timeLimitMs) { this.timeLimitMs = timeLimitMs; }

        public Integer getMemoryLimitMb() { return memoryLimitMb; }
        public void setMemoryLimitMb(Integer memoryLimitMb) { this.memoryLimitMb = memoryLimitMb; }

        public List<String> getAllowedLanguages() { return allowedLanguages; }
        public void setAllowedLanguages(List<String> allowedLanguages) { this.allowedLanguages = allowedLanguages; }
    }

    public static class Obo {
        private Boolean enabled;

        @JsonProperty("initialImageUrl")
        private String initialImageUrl;

        public Obo() {
        }

        public Boolean getEnabled() { return enabled; }
        public void setEnabled(Boolean enabled) { this.enabled = enabled; }

        public String getInitialImageUrl() { return initialImageUrl; }
        public void setInitialImageUrl(String initialImageUrl) { this.initialImageUrl = initialImageUrl; }
    }
}

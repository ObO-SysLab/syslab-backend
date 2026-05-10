package net.diveon.backend.domain.problem.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.diveon.backend.domain.problem.others.ForDtoTestCase;

import java.util.List;

public class ProblemCreateCodingRequest {

    private String title;
    private String summary;
    private String description;
    private String category;
    private String difficulty;
    private String visibility;
    private Long groupId;
    private Constraints constraints;

    @JsonProperty("inputDescription")
    private String inputDescription;

    @JsonProperty("outputDescription")
    private String outputDescription;

    private List<ForDtoTestCase> testcases;

    @JsonProperty("fileUrl")
    private String fileUrl;

    private Obo obo;

    public ProblemCreateCodingRequest() {
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

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }

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

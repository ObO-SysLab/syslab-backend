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
    private Constraints constraints;

    @JsonProperty("input_description")
    private String inputDescription;

    @JsonProperty("output_description")
    private String outputDescription;

    private List<ForDtoTestCase> testcases;

    @JsonProperty("file_url")
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
        @JsonProperty("time_limit_ms")
        private Integer timeLimitMs;

        @JsonProperty("memory_limit_mb")
        private Integer memoryLimitMb;

        @JsonProperty("allowed_languages")
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

        @JsonProperty("initial_image_url")
        private String initialImageUrl;

        public Obo() {
        }

        public Boolean getEnabled() { return enabled; }
        public void setEnabled(Boolean enabled) { this.enabled = enabled; }

        public String getInitialImageUrl() { return initialImageUrl; }
        public void setInitialImageUrl(String initialImageUrl) { this.initialImageUrl = initialImageUrl; }
    }
}

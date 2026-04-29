package net.diveon.backend.domain.problem.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class ProblemCreatePracticeRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String summary;

    @NotBlank
    private String description;

    @NotBlank
    private String category;

    @NotBlank
    private String difficulty;

    @NotBlank
    private String visibility;

    @NotNull
    @JsonProperty("vm_config")
    private VmConfig vmConfig;

    @NotBlank
    private String flag;

    @JsonProperty("docker_file_url")
    private String dockerFileUrl;

    public ProblemCreatePracticeRequest() {
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

    public VmConfig getVmConfig() { return vmConfig; }
    public void setVmConfig(VmConfig vmConfig) { this.vmConfig = vmConfig; }

    public String getFlag() { return flag; }
    public void setFlag(String flag) { this.flag = flag; }

    public String getDockerFileUrl() { return dockerFileUrl; }
    public void setDockerFileUrl(String dockerFileUrl) { this.dockerFileUrl = dockerFileUrl; }

    public static class VmConfig {

        @NotBlank
        @JsonProperty("os_image")
        private String osImage;

        @JsonProperty("allowed_commands")
        private List<String> allowedCommands;

        @JsonProperty("cpu_limit")
        private String cpuLimit = "0.5";

        @JsonProperty("memory_limit")
        private String memoryLimit = "512m";

        public VmConfig() {
        }

        public String getOsImage() { return osImage; }
        public void setOsImage(String osImage) { this.osImage = osImage; }

        public List<String> getAllowedCommands() { return allowedCommands; }
        public void setAllowedCommands(List<String> allowedCommands) { this.allowedCommands = allowedCommands; }

        public String getCpuLimit() { return cpuLimit; }
        public void setCpuLimit(String cpuLimit) { this.cpuLimit = cpuLimit; }

        public String getMemoryLimit() { return memoryLimit; }
        public void setMemoryLimit(String memoryLimit) { this.memoryLimit = memoryLimit; }
    }
}

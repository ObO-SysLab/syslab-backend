package net.diveon.backend.domain.problem.dto.request;

import java.util.List;

public class ProblemUpdatePracticeRequest {

    private String title;
    private String summary;
    private String description;
    private String category;
    private String difficulty;
    private String visibility;
    private VmConfig vmConfig;
    private String flag;
    private String dockerFileUrl;

    public ProblemUpdatePracticeRequest() {
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

        private String osImage;
        private List<String> allowedCommands;
        private String cpuLimit;
        private String memoryLimit;

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

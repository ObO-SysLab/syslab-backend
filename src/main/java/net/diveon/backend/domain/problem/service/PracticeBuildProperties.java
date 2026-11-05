package net.diveon.backend.domain.problem.service;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "practice.build")
public class PracticeBuildProperties {
    private boolean enabled = false;
    private boolean recoveryEnabled = false;
    @Min(1) @Max(1440) private int deadlineMinutes = 10;
    @Min(1) @Max(100) private int batchSize = 50;
    @Min(1) @Max(100) private int discoveryPages = 10;
    private String project = "syslab-practice-build";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean value) { enabled = value; }
    public boolean isRecoveryEnabled() { return recoveryEnabled; }
    public void setRecoveryEnabled(boolean value) { recoveryEnabled = value; }
    public int getDeadlineMinutes() { return deadlineMinutes; }
    public void setDeadlineMinutes(int value) { deadlineMinutes = value; }
    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int value) { batchSize = value; }
    public int getDiscoveryPages() { return discoveryPages; }
    public void setDiscoveryPages(int value) { discoveryPages = value; }
    public String getProject() { return project; }
    public void setProject(String value) { project = value; }
}

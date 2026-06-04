package net.diveon.backend.domain.problem.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import net.diveon.backend.domain.problem.others.ForDtoTestCase;
import org.hibernate.annotations.Type;

import java.util.List;

@Entity
@Table(name = "problem_coding")
public class ProblemCoding {

    @Id
    @Column(name = "prob_id")
    private Long probId;

    // 1:1 관계 설정: Problem의 ID를 그대로 자신의 PK로 사용 (@MapsId)
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "prob_id")
    private Problem problem;

    @Column(name = "summary", columnDefinition = "TEXT", nullable = false)
    private String summary;

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "input_description", columnDefinition = "TEXT", nullable = false)
    private String inputDescription;

    @Column(name = "output_description", columnDefinition = "TEXT", nullable = false)
    private String outputDescription;

    @Column(name = "time_limit_ms", nullable = false)
    private Integer timeLimitMs;

    @Column(name = "memory_limit_mb", nullable = false)
    private Integer memoryLimitMb;

    @Type(JsonType.class)
    @Column(name = "allowed_languages", columnDefinition = "JSONB", nullable = false)
    private List<String> allowedLanguages;

    @Type(JsonType.class)
    @Column(name = "testcases", columnDefinition = "JSONB", nullable = false)
    private List<ForDtoTestCase> testcases;

    @Column(name = "file_url", length = 500)
    private String fileUrl;

    @Column(name = "obo_enabled", nullable = false)
    private Boolean oboEnabled = false;

    @Column(name = "obo_initial_image_url", length = 500)
    private String oboInitialImageUrl;

    // 1. JPA용 기본 생성자
    public ProblemCoding() {
    }

    // 2. 데이터 생성을 위한 생성자
    public ProblemCoding(Problem problem, String summary, String description,
                         String inputDescription, String outputDescription,
                         Integer timeLimitMs, Integer memoryLimitMb,
                         List<String> allowedLanguages, List<ForDtoTestCase> testcases,
                         String fileUrl, Boolean oboEnabled, String oboInitialImageUrl) {
        this.problem = problem;
        this.summary = summary;
        this.description = description;
        this.inputDescription = inputDescription;
        this.outputDescription = outputDescription;
        this.timeLimitMs = timeLimitMs;
        this.memoryLimitMb = memoryLimitMb;
        this.allowedLanguages = allowedLanguages;
        this.testcases = testcases;
        this.fileUrl = fileUrl;
        this.oboEnabled = (oboEnabled != null) ? oboEnabled : false;
        this.oboInitialImageUrl = oboInitialImageUrl;
    }

    // 3. Getter 메서드
    public Long getProbId() { return probId; }
    public Problem getProblem() { return problem; }
    public String getSummary() { return summary; }
    public String getDescription() { return description; }
    public String getInputDescription() { return inputDescription; }
    public String getOutputDescription() { return outputDescription; }
    public Integer getTimeLimitMs() { return timeLimitMs; }
    public Integer getMemoryLimitMb() { return memoryLimitMb; }
    public List<String> getAllowedLanguages() { return allowedLanguages; }
    public List<ForDtoTestCase> getTestcases() { return testcases; }
    public String getFileUrl() { return fileUrl; }
    public Boolean getOboEnabled() { return oboEnabled; }
    public String getOboInitialImageUrl() { return oboInitialImageUrl; }

    // 4. Setter 메서드
    public void setProblem(Problem problem) { this.problem = problem; }
    public void setSummary(String summary) { this.summary = summary; }
    public void setDescription(String description) { this.description = description; }
    public void setInputDescription(String inputDescription) { this.inputDescription = inputDescription; }
    public void setOutputDescription(String outputDescription) { this.outputDescription = outputDescription; }
    public void setTimeLimitMs(Integer timeLimitMs) { this.timeLimitMs = timeLimitMs; }
    public void setMemoryLimitMb(Integer memoryLimitMb) { this.memoryLimitMb = memoryLimitMb; }
    public void setAllowedLanguages(List<String> allowedLanguages) { this.allowedLanguages = allowedLanguages; }
    public void setTestcases(List<ForDtoTestCase> testcases) { this.testcases = testcases; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public void setOboEnabled(Boolean oboEnabled) { this.oboEnabled = oboEnabled; }
    public void setOboInitialImageUrl(String oboInitialImageUrl) { this.oboInitialImageUrl = oboInitialImageUrl; }

    // 5. 비즈니스 로직
    public void updateProblemCoding(String summary, String description) {
        if (summary != null) this.summary = summary;
        if (description != null) this.description = description;
    }
}

package net.diveon.backend.domain.problem.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import org.hibernate.annotations.Type;

import java.util.List;

@Entity
@Table(name = "problem_practice")
public class ProblemPractice {
    // problem_practice 테이블의 prob_id는 PK이면서 동시에 problem_summary.id를 참조하는 FK임! Problem이 저장될 때 id가 자동으로 들어옴!
    @Id
    @Column(name = "prob_id")
    private Long probId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId // ProblemPractice가 별도 PK 안 만들고 Problem의 id를 그대로 자기 PK로 사용 왜냐?
            // Problem이랑 ProblemPractice는 1:1 관계니까. 문제 하나에 실습 정보 하나
    @JoinColumn(name = "prob_id")
    private Problem problem;

    @Column(name = "summary", columnDefinition = "TEXT", nullable = false) // 이 필드가 DB의 어떤 컬럼에 매핑되는지 설명
    private String summary;

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "os_image", length = 100, nullable = false)
    private String osImage;

    @Type(JsonType.class)
    @Column(name = "allowed_commands", columnDefinition = "JSONB")
    private List<String> allowedCommands;

    @Column(name = "cpu_limit", length = 10, nullable = false)
    private String cpuLimit;

    @Column(name = "memory_limit", length = 10, nullable = false)
    private String memoryLimit;

    @Column(name = "flag_hash", length = 255, nullable = false)
    private String flagHash;

    @Column(name = "docker_file_url", length = 500)
    private String dockerFileUrl;

    @Column(name = "hint", columnDefinition = "TEXT")
    private String hint;

    @Column(name = "ecr_image_uri", length = 500)
    private String ecrImageUri;

    @Column(name = "image_status", length = 20)
    private String imageStatus = "PENDING";

    @Column(name = "is_draft")
    private Boolean isDraft = false;

    @Column(name = "time_limit_sec")
    private Integer timeLimitSec;

    public ProblemPractice() {
    }

    public ProblemPractice(Problem problem, String summary, String description,
                           String osImage, List<String> allowedCommands,
                           String cpuLimit, String memoryLimit,
                           String flagHash, String dockerFileUrl) {
        this.problem = problem;
        this.summary = summary;
        this.description = description;
        this.osImage = osImage;
        this.allowedCommands = allowedCommands;
        this.cpuLimit = cpuLimit;
        this.memoryLimit = memoryLimit;
        this.flagHash = flagHash;
        this.dockerFileUrl = dockerFileUrl;
        this.isDraft = false;
    }

    public Long getProbId() { return probId; }
    public Problem getProblem() { return problem; }
    public String getSummary() { return summary; }
    public String getDescription() { return description; }
    public String getOsImage() { return osImage; }
    public List<String> getAllowedCommands() { return allowedCommands; }
    public String getCpuLimit() { return cpuLimit; }
    public String getMemoryLimit() { return memoryLimit; }
    public String getFlagHash() { return flagHash; }
    public String getDockerFileUrl() { return dockerFileUrl; }
    public String getHint() { return hint; }
    public String getEcrImageUri() { return ecrImageUri; }
    public String getImageStatus() { return imageStatus; }
    public void updateImageStatus(String imageStatus, String ecrImageUri) {
        this.imageStatus = imageStatus;
        if (ecrImageUri != null) this.ecrImageUri = ecrImageUri;
    }
    public Boolean getIsDraft() { return isDraft; }
    public Integer getTimeLimitSec() { return timeLimitSec; }

    public void updatePractice(String summary, String description, String osImage,
                               List<String> allowedCommands, String cpuLimit,
                               String memoryLimit, String flagHash, String dockerFileUrl) {
        if (summary != null) this.summary = summary;
        if (description != null) this.description = description;
        if (osImage != null) this.osImage = osImage;
        if (allowedCommands != null) this.allowedCommands = allowedCommands;
        if (cpuLimit != null) this.cpuLimit = cpuLimit;
        if (memoryLimit != null) this.memoryLimit = memoryLimit;
        if (flagHash != null) this.flagHash = flagHash;
        if (dockerFileUrl != null) this.dockerFileUrl = dockerFileUrl;
    }
}

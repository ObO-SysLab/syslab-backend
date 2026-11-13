package net.diveon.backend.domain.problem.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import org.hibernate.annotations.Type;

import java.util.List;
import java.time.Instant;
import java.util.Objects;

@Entity
@org.hibernate.annotations.DynamicUpdate
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

    // Commands the current image is restricted to; null = unrestricted. (The legacy os_image,
    // allowed_commands, cpu_limit and memory_limit columns were never used and are dropped.)
    @Type(JsonType.class)
    @Column(name = "command_allowlist", columnDefinition = "JSONB")
    private List<String> commandAllowlist;

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

    @Column(name = "build_request_id", length = 36)
    private String buildRequestId;
    @Column(name = "code_build_id", length = 255)
    private String codeBuildId;
    @Column(name = "build_requested_at")
    private Instant buildRequestedAt;
    @Column(name = "build_deadline_at")
    private Instant buildDeadlineAt;
    @Column(name = "build_failure_reason", length = 40)
    private String buildFailureReason;
    // Validated author-facing reason from the build ("CODE" or "CODE:command"); shown to the author only.
    @Column(name = "build_failure_detail", length = 80)
    private String buildFailureDetail;

    @Column(name = "is_draft")
    private Boolean isDraft = false;

    @Column(name = "time_limit_sec")
    private Integer timeLimitSec;

    /** Build outcome (and failure reason) when the image lacks the requested command restriction. */
    public static final String RESTRICTION_UNVERIFIED = "RESTRICTION_UNVERIFIED";

    public ProblemPractice() {
    }

    public ProblemPractice(Problem problem, String summary, String description,
                           String flagHash, String dockerFileUrl) {
        this.problem = problem;
        this.summary = summary;
        this.description = description;
        this.flagHash = flagHash;
        this.dockerFileUrl = dockerFileUrl;
        this.isDraft = false;
    }

    public Long getProbId() { return probId; }
    public Problem getProblem() { return problem; }
    public String getSummary() { return summary; }
    public String getDescription() { return description; }
    public List<String> getCommandAllowlist() { return commandAllowlist; }

    /** Set with the build request: the image built from that request carries this policy. */
    public void restrictCommands(List<String> normalizedPolicy) {
        if (!"PENDING".equals(imageStatus) || ecrImageUri != null)
            throw new IllegalStateException("Command policy is fixed by the build request");
        commandAllowlist = normalizedPolicy == null ? null : List.copyOf(normalizedPolicy);
    }
    public String getFlagHash() { return flagHash; }
    public String getDockerFileUrl() { return dockerFileUrl; }
    public String getHint() { return hint; }
    public String getEcrImageUri() { return ecrImageUri; }
    public String getImageStatus() { return imageStatus; }
    public String getBuildRequestId() { return buildRequestId; }
    public String getCodeBuildId() { return codeBuildId; }
    public Instant getBuildRequestedAt() { return buildRequestedAt; }
    public Instant getBuildDeadlineAt() { return buildDeadlineAt; }
    public String getBuildFailureReason() { return buildFailureReason; }
    public String getBuildFailureDetail() { return buildFailureDetail; }

    public void requestBuild(String requestId, Instant requestedAt, Instant deadlineAt) {
        if (requestId == null || !java.util.UUID.fromString(requestId).toString().equals(requestId)
                || requestedAt == null || deadlineAt == null || !deadlineAt.isAfter(requestedAt))
            throw new IllegalArgumentException("Invalid build request");
        buildRequestId = requestId;
        buildRequestedAt = requestedAt;
        buildDeadlineAt = deadlineAt;
        codeBuildId = null;
        buildFailureReason = null;
        buildFailureDetail = null;
        ecrImageUri = null;
        imageStatus = "PENDING";
    }

    /** Called only under the repository row lock; terminal state never regresses. */
    public boolean applyBuild(String requestId, String buildId, String status, String imageUri, Instant now) {
        return applyBuild(requestId, buildId, status, imageUri, now, null);
    }

    public boolean applyBuild(String requestId, String buildId, String status, String imageUri, Instant now,
            String failureDetail) {
        if (!"PENDING".equals(imageStatus) || !Objects.equals(buildRequestId, requestId)
                || buildDeadlineAt == null) return false;
        if (!now.isBefore(buildDeadlineAt)) {
            imageStatus = "FAILED";
            buildFailureReason = "DEADLINE_EXCEEDED";
            return true;
        }
        if (codeBuildId != null && !codeBuildId.equals(buildId)) return false;
        if (!java.util.Set.of("IN_PROGRESS", "SUCCEEDED", "FAILED", "FAULT", "STOPPED", "TIMED_OUT",
                RESTRICTION_UNVERIFIED).contains(status))
            throw new IllegalArgumentException("Unknown build status");
        if ("SUCCEEDED".equals(status) && (imageUri == null || !imageUri.matches(".+@sha256:[0-9a-f]{64}")))
            throw new IllegalArgumentException("Verified image digest required");
        codeBuildId = Objects.requireNonNull(buildId);
        if ("SUCCEEDED".equals(status)) {
            imageStatus = "READY";
            ecrImageUri = imageUri;
        } else if (!"IN_PROGRESS".equals(status)) {
            imageStatus = "FAILED";
            buildFailureReason = status;
            buildFailureDetail = failureDetail != null && failureDetail.length() <= 80 ? failureDetail : null;
        }
        return true;
    }
    public void updateImageStatus(String imageStatus, String ecrImageUri) {
        this.imageStatus = imageStatus;
        if (ecrImageUri != null) this.ecrImageUri = ecrImageUri;
    }
    public Boolean getIsDraft() { return isDraft; }
    public Integer getTimeLimitSec() { return timeLimitSec; }

    /** Optional hint learners can open on the problem page; blank clears it. */
    public void describeHint(String hint) {
        this.hint = hint == null || hint.isBlank() ? null : hint.strip();
    }

    public void updatePractice(String summary, String description) {
        if (summary != null) this.summary = summary;
        if (description != null) this.description = description;
    }
}

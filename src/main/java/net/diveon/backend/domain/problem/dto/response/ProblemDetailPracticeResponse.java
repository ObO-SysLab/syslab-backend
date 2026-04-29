package net.diveon.backend.domain.problem.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.diveon.backend.domain.problem.dto.response.interfaces.ProblemDetailResponse;
import net.diveon.backend.domain.problem.entity.Problem;
import net.diveon.backend.domain.problem.entity.ProblemPractice;

import java.util.List;

public class ProblemDetailPracticeResponse implements ProblemDetailResponse {

    @JsonProperty("prob_id")
    private Long probId;

    private String author;
    private String type;
    private String title;
    private String category;
    private String difficulty;
    private String visibility;
    private String summary;
    private String description;

    @JsonProperty("vm_info")
    private VmInfo vmInfo;

    @JsonProperty("solved_count")
    private Integer solvedCount;

    @JsonProperty("submitted_count")
    private Integer submittedCount;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;

    public ProblemDetailPracticeResponse() {
    }

    public ProblemDetailPracticeResponse(
        Long probId,
        String author,
        String type,
        String title,
        String category,
        String difficulty,
        String visibility,
        String summary,
        String description,
        VmInfo vmInfo,
        Integer solvedCount,
        Integer submittedCount,
        String createdAt,
        String updatedAt
    ) {
        this.probId = probId;
        this.author = author;
        this.type = type;
        this.title = title;
        this.category = category;
        this.difficulty = difficulty;
        this.visibility = visibility;
        this.summary = summary;
        this.description = description;
        this.vmInfo = vmInfo;
        this.solvedCount = solvedCount;
        this.submittedCount = submittedCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ProblemDetailPracticeResponse of(Problem problem, ProblemPractice problemPractice) {
        VmInfo vmInfo = new VmInfo(
            problemPractice.getOsImage(),
            problemPractice.getAllowedCommands()
        );

        return new ProblemDetailPracticeResponse(
            problem.getId(),
            problem.getAuthor().getNickname(),
            problem.getType(),
            problem.getTitle(),
            problem.getCategory(),
            problem.getDifficulty(),
            problem.getVisibility(),
            problemPractice.getSummary(),
            problemPractice.getDescription(),
            vmInfo,
            problem.getSolvedCount(),
            problem.getSubmittedCount(),
            problem.getCreatedAt().toString(),
            problem.getUpatedAt().toString()
        );
    }

    public Long getProbId() { return probId; }
    public String getAuthor() { return author; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getCategory() { return category; }
    public String getDifficulty() { return difficulty; }
    public String getVisibility() { return visibility; }
    public String getSummary() { return summary; }
    public String getDescription() { return description; }
    public VmInfo getVmInfo() { return vmInfo; }
    public Integer getSolvedCount() { return solvedCount; }
    public Integer getSubmittedCount() { return submittedCount; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }

    public static class VmInfo {

        @JsonProperty("os_image")
        private String osImage;

        @JsonProperty("allowed_commands")
        private List<String> allowedCommands;

        public VmInfo() {
        }

        public VmInfo(String osImage, List<String> allowedCommands) {
            this.osImage = osImage;
            this.allowedCommands = allowedCommands;
        }

        public String getOsImage() { return osImage; }
        public List<String> getAllowedCommands() { return allowedCommands; }
    }
}

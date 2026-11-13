package net.diveon.backend.domain.problem.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.diveon.backend.domain.problem.dto.response.interfaces.ProblemDetailResponse;
import net.diveon.backend.domain.problem.entity.Problem;
import net.diveon.backend.domain.problem.entity.ProblemPractice;

import java.util.List;

public class ProblemDetailPracticeResponse implements ProblemDetailResponse {

    @JsonProperty("probId")
    private Long probId;

    private String author;

    @JsonProperty("isOwner")
    private boolean isOwner;

    private String type;
    private String title;
    private String category;
    private String difficulty;
    private String visibility;
    private String summary;
    private String description;

    @JsonProperty("vmInfo")
    private VmInfo vmInfo;

    private PracticeBuildStatusResponse practiceBuild;

    @JsonProperty("solvedCount")
    private Integer solvedCount;

    @JsonProperty("submittedCount")
    private Integer submittedCount;

    @JsonProperty("createdAt")
    private String createdAt;

    @JsonProperty("updatedAt")
    private String updatedAt;

    public ProblemDetailPracticeResponse() {
    }

    public ProblemDetailPracticeResponse(
        Long probId,
        String author,
        boolean isOwner,
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
        this.isOwner = isOwner;
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

    public static ProblemDetailPracticeResponse of(Problem problem, ProblemPractice problemPractice, boolean isOwner, boolean canExecute) {
        VmInfo vmInfo = new VmInfo(problemPractice.getCommandAllowlist());

        ProblemDetailPracticeResponse response = new ProblemDetailPracticeResponse(
            problem.getId(),
            problem.getAuthor().getNickname(),
            isOwner,
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
        response.practiceBuild = PracticeBuildStatusResponse.of(problemPractice, canExecute, isOwner);
        response.hint = problemPractice.getHint();
        return response;
    }

    // Optional author hint; null when there is none.
    private String hint;

    public String getHint() { return hint; }

    public Long getProbId() { return probId; }
    public String getAuthor() { return author; }
    public boolean getIsOwner() { return isOwner; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getCategory() { return category; }
    public String getDifficulty() { return difficulty; }
    public String getVisibility() { return visibility; }
    public String getSummary() { return summary; }
    public String getDescription() { return description; }
    public VmInfo getVmInfo() { return vmInfo; }
    public PracticeBuildStatusResponse getPracticeBuild() { return practiceBuild; }
    public Integer getSolvedCount() { return solvedCount; }
    public Integer getSubmittedCount() { return submittedCount; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }

    public static class VmInfo {

        // Commands the practice image actually allows; null = unrestricted.
        @JsonProperty("commandAllowlist")
        private List<String> commandAllowlist;

        public VmInfo() {
        }

        public VmInfo(List<String> commandAllowlist) {
            this.commandAllowlist = commandAllowlist;
        }

        public List<String> getCommandAllowlist() { return commandAllowlist; }
    }
}

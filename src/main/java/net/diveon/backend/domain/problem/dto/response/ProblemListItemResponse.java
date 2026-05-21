package net.diveon.backend.domain.problem.dto.response;

import net.diveon.backend.domain.problem.entity.Problem;

public class ProblemListItemResponse {

    private Long probId;
    private String type;
    private String title;
    private String category;
    private String difficulty;
    private Integer solvedCount;

    public ProblemListItemResponse() {
    }

    public ProblemListItemResponse(Long probId, String type, String title, String category, String difficulty, Integer solvedCount) {
        this.probId = probId;
        this.type = type;
        this.title = title;
        this.category = category;
        this.difficulty = difficulty;
        this.solvedCount = solvedCount;
    }

    public static ProblemListItemResponse of(Problem problem) {
        return new ProblemListItemResponse(
            problem.getId(),
            problem.getType(),
            problem.getTitle(),
            problem.getCategory(),
            problem.getDifficulty(),
            problem.getSolvedCount()
        );
    }

    public Long getProbId() {
        return probId;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getCategory() {
        return category;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public Integer getSolvedCount() {
        return solvedCount;
    }
}

package net.diveon.backend.domain.problem.dto.response;

import net.diveon.backend.domain.problem.entity.Problem;

public class ProblemListItemResponse {

    private Long probId;
    private String title;
    private String category;
    private String difficulty;
    private Integer solvedCount;

    public ProblemListItemResponse() {
    }

    public ProblemListItemResponse(Long probId, String title, String category, String difficulty, Integer solvedCount) {
        this.probId = probId;
        this.title = title;
        this.category = category;
        this.difficulty = difficulty;
        this.solvedCount = solvedCount;
    }

    public static ProblemListItemResponse of(Problem problem) {
        return new ProblemListItemResponse(
            problem.getId(),
            problem.getTitle(),
            problem.getCategory(),
            problem.getDifficulty(),
            problem.getSolvedCount()
        );
    }

    public Long getProbId() {
        return probId;
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

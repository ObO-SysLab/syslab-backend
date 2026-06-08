package net.diveon.backend.domain.problem.dto.response;

import net.diveon.backend.domain.problem.entity.Problem;

public class ProblemListItemResponse {

    private Long probId;
    private String type;
    private String title;
    private String author;
    private String category;
    private String difficulty;
    private Integer solvedCount;
    private Boolean isSolved;

    public ProblemListItemResponse() {
    }

    public ProblemListItemResponse(Long probId, String type, String title, String author, String category, String difficulty, Integer solvedCount, Boolean isSolved) {
        this.probId = probId;
        this.type = type;
        this.title = title;
        this.author = author;
        this.category = category;
        this.difficulty = difficulty;
        this.solvedCount = solvedCount;
        this.isSolved = isSolved;
    }

    public static ProblemListItemResponse of(Problem problem, boolean isSolved) {
        return new ProblemListItemResponse(
            problem.getId(),
            problem.getType(),
            problem.getTitle(),
            problem.getAuthor().getNickname(),
            problem.getCategory(),
            problem.getDifficulty(),
            problem.getSolvedCount(),
            isSolved
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

    public String getAuthor() {
        return author;
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

    public Boolean getIsSolved() {
        return isSolved;
    }
}

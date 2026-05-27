package net.diveon.backend.domain.contest.others;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ForDtoContestProblem {

    private String id;

    @JsonProperty("contestProblemId")
    private Long contestProblemId;

    @JsonProperty("problemId")
    private Long problemId;

    private String title;
    private Integer points;
    private Integer solvedCount;
    private String category;

    @JsonProperty("isSolved")
    private Boolean isSolved;

    public ForDtoContestProblem() {
    }

    public ForDtoContestProblem(String id, Long contestProblemId, Long problemId, String title, Integer points,
                                Integer solvedCount, String category, Boolean isSolved) {
        this.id = id;
        this.contestProblemId = contestProblemId;
        this.problemId = problemId;
        this.title = title;
        this.points = points;
        this.solvedCount = solvedCount;
        this.category = category;
        this.isSolved = isSolved;
    }

    public String getId() { return id; }
    public Long getContestProblemId() { return contestProblemId; }
    public Long getProblemId() { return problemId; }
    public String getTitle() { return title; }
    public Integer getPoints() { return points; }
    public Integer getSolvedCount() { return solvedCount; }
    public String getCategory() { return category; }
    public Boolean getIsSolved() { return isSolved; }

    public void setId(String id) { this.id = id; }
    public void setContestProblemId(Long contestProblemId) { this.contestProblemId = contestProblemId; }
    public void setProblemId(Long problemId) { this.problemId = problemId; }
    public void setTitle(String title) { this.title = title; }
    public void setPoints(Integer points) { this.points = points; }
    public void setSolvedCount(Integer solvedCount) { this.solvedCount = solvedCount; }
    public void setCategory(String category) { this.category = category; }
    public void setIsSolved(Boolean isSolved) { this.isSolved = isSolved; }
}

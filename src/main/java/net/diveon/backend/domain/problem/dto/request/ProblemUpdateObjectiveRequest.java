package net.diveon.backend.domain.problem.dto.request;

public class ProblemUpdateObjectiveRequest {

    private String title;
    private String summary;
    private String description;
    private String difficulty;

    public ProblemUpdateObjectiveRequest() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
}

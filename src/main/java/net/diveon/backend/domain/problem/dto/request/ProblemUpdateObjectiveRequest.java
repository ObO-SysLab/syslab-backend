package net.diveon.backend.domain.problem.dto.request;

import com.fasterxml.jackson.databind.JsonNode;

public class ProblemUpdateObjectiveRequest {

    private String title;
    private String summary;
    private String description;
    private String difficulty;
    private JsonNode oboJson;

    public ProblemUpdateObjectiveRequest() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public JsonNode getOboJson() { return oboJson; }
    public void setOboJson(JsonNode oboJson) { this.oboJson = oboJson; }
}

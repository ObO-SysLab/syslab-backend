package net.diveon.backend.domain.problem.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ProblemUpdatePracticeResponse {

    @JsonProperty("prob_id")
    private long probId;

    @JsonProperty("type")
    private String type;

    @JsonProperty("title")
    private String title;

    @JsonProperty("updated_at")
    private String updatedAt;

    public ProblemUpdatePracticeResponse() {
    }

    public ProblemUpdatePracticeResponse(long probId, String type, String title, String updatedAt) {
        this.probId = probId;
        this.type = type;
        this.title = title;
        this.updatedAt = updatedAt;
    }

    public long getProbId() { return probId; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getUpdatedAt() { return updatedAt; }
}

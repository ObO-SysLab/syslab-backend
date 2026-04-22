package net.diveon.backend.domain.problem.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ProblemCreatePracticeResponse {

    @JsonProperty("prob_id")
    private long probId;

    @JsonProperty("type")
    private String type;

    @JsonProperty("title")
    private String title;

    @JsonProperty("created_at")
    private String createdAt;

    public ProblemCreatePracticeResponse() {
    }

    public ProblemCreatePracticeResponse(long probId, String type, String title, String createdAt) {
        this.probId = probId;
        this.type = type;
        this.title = title;
        this.createdAt = createdAt;
    }

    public long getProbId() { return probId; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getCreatedAt() { return createdAt; }
}

// @JsonProperty - Java 필드명(probId)을 JSON 키(prob_id)로 바꿔줌
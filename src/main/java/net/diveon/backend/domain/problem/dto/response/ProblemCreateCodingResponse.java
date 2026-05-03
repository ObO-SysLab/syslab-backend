package net.diveon.backend.domain.problem.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ProblemCreateCodingResponse {

    @JsonProperty("probId")
    private Long probId;

    private String type;
    private String title;

    @JsonProperty("createdAt")
    private String createdAt;

    public ProblemCreateCodingResponse() {
    }

    public ProblemCreateCodingResponse(Long probId, String type, String title, String createdAt) {
        this.probId = probId;
        this.type = type;
        this.title = title;
        this.createdAt = createdAt;
    }

    public Long getProbId() { return probId; }
    public void setProbId(Long probId) { this.probId = probId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}

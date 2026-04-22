package net.diveon.backend.domain.problem.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ProblemCreatePracticeResponse {

    @JsonProperty("prob_id") //  Java 필드명 probId를 JSON 키 prob_id로 변환 (다시 프론트한테 돌려줘야 되니까)
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


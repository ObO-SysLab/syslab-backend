package net.diveon.backend.domain.problem.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ProblemCommentUpdateRequest {

    private String content;

    @JsonProperty("isPrivate")
    private Boolean isPrivate;

    public ProblemCommentUpdateRequest() {
    }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Boolean getIsPrivate() { return isPrivate; }
    public void setIsPrivate(Boolean isPrivate) { this.isPrivate = isPrivate; }
}

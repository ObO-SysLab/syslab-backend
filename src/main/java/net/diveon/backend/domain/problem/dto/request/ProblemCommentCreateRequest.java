package net.diveon.backend.domain.problem.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public class ProblemCommentCreateRequest {

    @NotBlank
    private String content;

    @JsonProperty("isPrivate")
    private Boolean isPrivate = false;

    @JsonProperty("parentId")
    private Long parentId;

    public ProblemCommentCreateRequest() {
    }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Boolean getIsPrivate() { return isPrivate; }
    public void setIsPrivate(Boolean isPrivate) { this.isPrivate = isPrivate; }

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
}

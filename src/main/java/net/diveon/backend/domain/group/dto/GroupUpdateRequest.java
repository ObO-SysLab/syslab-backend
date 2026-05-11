package net.diveon.backend.domain.group.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class GroupUpdateRequest {

    @NotBlank
    private String title;

    private String description;

    private List<String> tags;

    @NotNull
    private Boolean isPrivate;

    @NotNull
    private Boolean isAutoApprove;

    public GroupUpdateRequest() {}

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public List<String> getTags() { return tags; }
    public Boolean getIsPrivate() { return isPrivate; }
    public Boolean getIsAutoApprove() { return isAutoApprove; }
}

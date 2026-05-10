package net.diveon.backend.domain.group.dto;

public class GroupCreateResponse {

    private final Long groupId;

    public GroupCreateResponse(Long groupId) {
        this.groupId = groupId;
    }

    public Long getGroupId() { return groupId; }
}

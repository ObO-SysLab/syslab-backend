package net.diveon.backend.domain.group.dto;

import net.diveon.backend.domain.group.entity.GroupUser;

public class GroupMyListResponse {

    private Long groupId;
    private String title;
    private boolean isAlreadyAdded;

    public GroupMyListResponse(Long groupId, String title, boolean isAlreadyAdded) {
        this.groupId = groupId;
        this.title = title;
        this.isAlreadyAdded = isAlreadyAdded;
    }

    public static GroupMyListResponse of(GroupUser groupUser, boolean isAlreadyAdded) {
        return new GroupMyListResponse(
                groupUser.getGroup().getId(),
                groupUser.getGroup().getTitle(),
                isAlreadyAdded
        );
    }

    public Long getGroupId() { return groupId; }
    public String getTitle() { return title; }
    public boolean isAlreadyAdded() { return isAlreadyAdded; }
}

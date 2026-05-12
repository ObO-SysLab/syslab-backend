package net.diveon.backend.domain.group.dto;

import net.diveon.backend.domain.group.entity.GroupUser;

public class GroupMyListResponse {

    private Long groupId;
    private String title;

    public GroupMyListResponse(Long groupId, String title) {
        this.groupId = groupId;
        this.title = title;
    }

    public static GroupMyListResponse of(GroupUser groupUser) {
        return new GroupMyListResponse(
                groupUser.getGroup().getId(),
                groupUser.getGroup().getTitle()
        );
    }

    public Long getGroupId() { return groupId; }
    public String getTitle() { return title; }
}

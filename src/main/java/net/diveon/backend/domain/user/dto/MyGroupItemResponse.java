package net.diveon.backend.domain.user.dto;

import net.diveon.backend.domain.group.entity.GroupUser;

public class MyGroupItemResponse {

    private final Long groupId;
    private final String title;
    private final String image;
    private final String role;
    private final Boolean isPrivate;
    private final Long memberCount;

    public MyGroupItemResponse(Long groupId, String title, String image, String role, Boolean isPrivate, Long memberCount) {
        this.groupId = groupId;
        this.title = title;
        this.image = image;
        this.role = role;
        this.isPrivate = isPrivate;
        this.memberCount = memberCount;
    }

    public static MyGroupItemResponse of(GroupUser gu, Long memberCount) {
        return new MyGroupItemResponse(
            gu.getGroup().getId(),
            gu.getGroup().getTitle(),
            gu.getGroup().getImage(),
            gu.getRole().name(),
            gu.getGroup().getIsPrivate(),
            memberCount
        );
    }

    public Long getGroupId() { return groupId; }
    public String getTitle() { return title; }
    public String getImage() { return image; }
    public String getRole() { return role; }
    public Boolean getIsPrivate() { return isPrivate; }
    public Long getMemberCount() { return memberCount; }
}

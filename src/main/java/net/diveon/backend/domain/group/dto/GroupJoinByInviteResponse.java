package net.diveon.backend.domain.group.dto;

public class GroupJoinByInviteResponse {
    private Long userId;
    private String newStatus;
    private Long groupId;
    private String groupTitle;

    public GroupJoinByInviteResponse(Long userId, String newStatus, Long groupId, String groupTitle) {
        this.userId = userId;
        this.newStatus = newStatus;
        this.groupId = groupId;
        this.groupTitle = groupTitle;
    }

    public Long getUserId() { return userId; }
    public String getNewStatus() { return newStatus; }
    public Long getGroupId() { return groupId; }
    public String getGroupTitle() { return groupTitle; }
}

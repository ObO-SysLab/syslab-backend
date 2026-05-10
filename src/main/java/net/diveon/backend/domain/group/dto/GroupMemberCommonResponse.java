package net.diveon.backend.domain.group.dto;

public class GroupMemberCommonResponse {

    private final Long userId;
    private final String newStatus;

    public GroupMemberCommonResponse(Long userId, String newStatus) {
        this.userId = userId;
        this.newStatus = newStatus;
    }

    public Long getUserId() { return userId; }
    public String getNewStatus() { return newStatus; }
}

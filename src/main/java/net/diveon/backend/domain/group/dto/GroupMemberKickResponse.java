package net.diveon.backend.domain.group.dto;

public class GroupMemberKickResponse {

    private final Long kickedUserId;

    public GroupMemberKickResponse(Long kickedUserId) {
        this.kickedUserId = kickedUserId;
    }

    public Long getKickedUserId() { return kickedUserId; }
}

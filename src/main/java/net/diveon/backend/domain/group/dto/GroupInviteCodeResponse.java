package net.diveon.backend.domain.group.dto;

import java.time.LocalDateTime;

public class GroupInviteCodeResponse {
    private String invitationCode;
    private LocalDateTime expiresAt;

    public GroupInviteCodeResponse(String invitationCode, LocalDateTime expiresAt) {
        this.invitationCode = invitationCode;
        this.expiresAt = expiresAt;
    }

    public String getInvitationCode() { return invitationCode; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
}

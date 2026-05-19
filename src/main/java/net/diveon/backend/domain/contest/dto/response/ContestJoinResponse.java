package net.diveon.backend.domain.contest.dto.response;

public class ContestJoinResponse {

    private Boolean isJoined;

    public ContestJoinResponse(Boolean isJoined) {
        this.isJoined = isJoined;
    }

    public Boolean getIsJoined() { return isJoined; }
}

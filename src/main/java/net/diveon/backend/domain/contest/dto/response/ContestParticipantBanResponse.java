package net.diveon.backend.domain.contest.dto.response;

public class ContestParticipantBanResponse {

    private final Boolean isBanned;

    public ContestParticipantBanResponse(Boolean isBanned) {
        this.isBanned = isBanned;
    }

    public Boolean getIsBanned() {
        return isBanned;
    }
}

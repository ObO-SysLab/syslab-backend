package net.diveon.backend.domain.contest.dto.response;

public class ContestLeadershipTransferResponse {

    private final String previousLeaderNickname;
    private final String newLeaderNickname;

    public ContestLeadershipTransferResponse(String previousLeaderNickname, String newLeaderNickname) {
        this.previousLeaderNickname = previousLeaderNickname;
        this.newLeaderNickname = newLeaderNickname;
    }

    public String getPreviousLeaderNickname() {
        return previousLeaderNickname;
    }

    public String getNewLeaderNickname() {
        return newLeaderNickname;
    }
}

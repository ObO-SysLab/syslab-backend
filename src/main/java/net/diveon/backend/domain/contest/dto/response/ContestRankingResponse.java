package net.diveon.backend.domain.contest.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

public class ContestRankingResponse {

    private final Long contestId;
    @JsonProperty("isOngoing")
    private final Boolean isOngoing;
    private final List<RankingEntry> rankings;

    public ContestRankingResponse(Long contestId, Boolean isOngoing, List<RankingEntry> rankings) {
        this.contestId = contestId;
        this.isOngoing = isOngoing;
        this.rankings = rankings;
    }

    public Long getContestId() { return contestId; }
    public Boolean getIsOngoing() { return isOngoing; }
    public List<RankingEntry> getRankings() { return rankings; }

    public static class RankingEntry {
        private final Integer rank;
        private final Long userId;
        private final String nickname;
        private final Integer score;
        private final LocalDateTime lastSolvedAt;
        private final Integer solvedCount;

        public RankingEntry(Integer rank, Long userId, String nickname,
                            Integer score, LocalDateTime lastSolvedAt, Integer solvedCount) {
            this.rank = rank;
            this.userId = userId;
            this.nickname = nickname;
            this.score = score;
            this.lastSolvedAt = lastSolvedAt;
            this.solvedCount = solvedCount;
        }

        public Integer getRank() { return rank; }
        public Long getUserId() { return userId; }
        public String getNickname() { return nickname; }
        public Integer getScore() { return score; }
        public LocalDateTime getLastSolvedAt() { return lastSolvedAt; }
        public Integer getSolvedCount() { return solvedCount; }
    }
}

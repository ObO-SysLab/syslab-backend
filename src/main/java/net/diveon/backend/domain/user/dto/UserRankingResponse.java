package net.diveon.backend.domain.user.dto;

import java.time.LocalDateTime;
import java.util.List;

public class UserRankingResponse {

    private final long totalElements;
    private final int totalPages;
    private final int currentPage;
    private final LocalDateTime calculatedAt;
    private final RankingEntry myRanking;
    private final List<RankingEntry> rankings;

    public UserRankingResponse(long totalElements, int totalPages, int currentPage,
                               LocalDateTime calculatedAt, RankingEntry myRanking,
                               List<RankingEntry> rankings) {
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.currentPage = currentPage;
        this.calculatedAt = calculatedAt;
        this.myRanking = myRanking;
        this.rankings = rankings;
    }

    public long getTotalElements() { return totalElements; }
    public int getTotalPages() { return totalPages; }
    public int getCurrentPage() { return currentPage; }
    public LocalDateTime getCalculatedAt() { return calculatedAt; }
    public RankingEntry getMyRanking() { return myRanking; }
    public List<RankingEntry> getRankings() { return rankings; }

    public static class RankingEntry {
        private final Integer rank;
        private final Long userId;
        private final String nickname;
        private final String profileImgUrl;
        private final Integer tier;
        private final Integer score;

        public RankingEntry(Integer rank, Long userId, String nickname, String profileImgUrl, Integer tier, Integer score) {
            this.rank = rank;
            this.userId = userId;
            this.nickname = nickname;
            this.profileImgUrl = profileImgUrl;
            this.tier = tier;
            this.score = score;
        }

        public Integer getRank() { return rank; }
        public Long getUserId() { return userId; }
        public String getNickname() { return nickname; }
        public String getProfileImgUrl() { return profileImgUrl; }
        public Integer getTier() { return tier; }
        public Integer getScore() { return score; }
    }
}

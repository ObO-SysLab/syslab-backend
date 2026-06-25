package net.diveon.backend.domain.problem.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public class ProblemRankingResponse {

    private final long totalElements;
    private final int totalPages;
    private final int currentPage;
    private final List<RankingEntry> rankings;

    public ProblemRankingResponse(long totalElements, int totalPages, int currentPage, List<RankingEntry> rankings) {
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.currentPage = currentPage;
        this.rankings = rankings;
    }

    public long getTotalElements() { return totalElements; }
    public int getTotalPages() { return totalPages; }
    public int getCurrentPage() { return currentPage; }
    public List<RankingEntry> getRankings() { return rankings; }

    public static class RankingEntry {
        private final Integer rank;
        private final Long userId;
        private final String nickname;
        private final String profileImgUrl;
        private final LocalDateTime firstSolvedAt;
        private final String language;

        public RankingEntry(Integer rank, Long userId, String nickname, String profileImgUrl,
                            LocalDateTime firstSolvedAt, String language) {
            this.rank = rank;
            this.userId = userId;
            this.nickname = nickname;
            this.profileImgUrl = profileImgUrl;
            this.firstSolvedAt = firstSolvedAt;
            this.language = language;
        }

        public Integer getRank() { return rank; }
        public Long getUserId() { return userId; }
        public String getNickname() { return nickname; }
        public String getProfileImgUrl() { return profileImgUrl; }
        public LocalDateTime getFirstSolvedAt() { return firstSolvedAt; }
        public String getLanguage() { return language; }
    }
}

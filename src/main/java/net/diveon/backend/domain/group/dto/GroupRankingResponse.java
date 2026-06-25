package net.diveon.backend.domain.group.dto;

import java.util.List;

public class GroupRankingResponse {

    private final long totalElements;
    private final int totalPages;
    private final int currentPage;
    private final List<RankingEntry> rankings;

    public GroupRankingResponse(long totalElements, int totalPages, int currentPage, List<RankingEntry> rankings) {
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
        private final Long groupId;
        private final String title;
        private final String image;
        private final Long memberCount;
        private final Long score;

        public RankingEntry(Integer rank, Long groupId, String title, String image,
                            Long memberCount, Long score) {
            this.rank = rank;
            this.groupId = groupId;
            this.title = title;
            this.image = image;
            this.memberCount = memberCount;
            this.score = score;
        }

        public Integer getRank() { return rank; }
        public Long getGroupId() { return groupId; }
        public String getTitle() { return title; }
        public String getImage() { return image; }
        public Long getMemberCount() { return memberCount; }
        public Long getScore() { return score; }
    }
}

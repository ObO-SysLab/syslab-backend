package net.diveon.backend.domain.contest.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public class ContestListResponse {

    private Long totalContests;
    private Integer currentPage;
    private Integer totalPages;
    private List<ContestItem> contests;

    public ContestListResponse(Long totalContests, Integer currentPage, Integer totalPages, List<ContestItem> contests) {
        this.totalContests = totalContests;
        this.currentPage = currentPage;
        this.totalPages = totalPages;
        this.contests = contests;
    }

    public Long getTotalContests() { return totalContests; }
    public Integer getCurrentPage() { return currentPage; }
    public Integer getTotalPages() { return totalPages; }
    public List<ContestItem> getContests() { return contests; }

    public static class ContestItem {
        private Long contestId;
        private String title;
        private Long participants;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String type;
        private String prize;
        private String status;
        private Boolean isHot;
        private Boolean isJoined;

        public ContestItem(Long contestId, String title, Long participants,
                           LocalDateTime startTime, LocalDateTime endTime,
                           String type, String prize, String status,
                           Boolean isHot, Boolean isJoined) {
            this.contestId = contestId;
            this.title = title;
            this.participants = participants;
            this.startTime = startTime;
            this.endTime = endTime;
            this.type = type;
            this.prize = prize;
            this.status = status;
            this.isHot = isHot;
            this.isJoined = isJoined;
        }

        public Long getContestId() { return contestId; }
        public String getTitle() { return title; }
        public Long getParticipants() { return participants; }
        public LocalDateTime getStartTime() { return startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public String getType() { return type; }
        public String getPrize() { return prize; }
        public String getStatus() { return status; }
        public Boolean getIsHot() { return isHot; }
        public Boolean getIsJoined() { return isJoined; }
    }
}

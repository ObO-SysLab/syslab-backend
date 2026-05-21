package net.diveon.backend.domain.contest.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public class GroupContestListResponse {

    private Integer currentPage;
    private Integer totalPages;
    private List<ContestItem> contests;

    public GroupContestListResponse(Integer currentPage, Integer totalPages, List<ContestItem> contests) {
        this.currentPage = currentPage;
        this.totalPages = totalPages;
        this.contests = contests;
    }

    public Integer getCurrentPage() { return currentPage; }
    public Integer getTotalPages() { return totalPages; }
    public List<ContestItem> getContests() { return contests; }

    public static class ContestItem {
        private Long contestId;
        private String title;
        private String host;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String status;
        private Long participantCount;

        public ContestItem(Long contestId, String title, String host,
                           LocalDateTime startTime, LocalDateTime endTime,
                           String status, Long participantCount) {
            this.contestId = contestId;
            this.title = title;
            this.host = host;
            this.startTime = startTime;
            this.endTime = endTime;
            this.status = status;
            this.participantCount = participantCount;
        }

        public Long getContestId() { return contestId; }
        public String getTitle() { return title; }
        public String getHost() { return host; }
        public LocalDateTime getStartTime() { return startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public String getStatus() { return status; }
        public Long getParticipantCount() { return participantCount; }
    }
}

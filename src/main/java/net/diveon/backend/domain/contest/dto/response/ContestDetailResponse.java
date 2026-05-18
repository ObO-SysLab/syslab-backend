package net.diveon.backend.domain.contest.dto.response;

import java.time.LocalDateTime;

public class ContestDetailResponse {

    private Long contestId;
    private String title;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private Integer progress;
    private Long totalUser;
    private UserContext userContext;

    public ContestDetailResponse(Long contestId, String title, String description,
                                  LocalDateTime startTime, LocalDateTime endTime,
                                  String status, Integer progress, Long totalUser,
                                  UserContext userContext) {
        this.contestId = contestId;
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.progress = progress;
        this.totalUser = totalUser;
        this.userContext = userContext;
    }

    public Long getContestId() { return contestId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public String getStatus() { return status; }
    public Integer getProgress() { return progress; }
    public Long getTotalUser() { return totalUser; }
    public UserContext getUserContext() { return userContext; }

    public static class UserContext {
        private Boolean isJoined;
        private Boolean isContestLeader;
        private Integer myScore;
        private Long myRank;

        public UserContext(Boolean isJoined, Boolean isContestLeader, Integer myScore, Long myRank) {
            this.isJoined = isJoined;
            this.isContestLeader = isContestLeader;
            this.myScore = myScore;
            this.myRank = myRank;
        }

        public Boolean getIsJoined() { return isJoined; }
        public Boolean getIsContestLeader() { return isContestLeader; }
        public Integer getMyScore() { return myScore; }
        public Long getMyRank() { return myRank; }
    }
}

package net.diveon.backend.domain.group.dto;

import java.util.List;

public class GroupDetailResponse {

    private final Long groupId;
    private final String title;
    private final String description;
    private final List<String> tags;
    private final Stats stats;
    private final Settings settings;
    private final UserContext userContext;

    public GroupDetailResponse(Long groupId, String title, String description, List<String> tags,
                               Stats stats, Settings settings, UserContext userContext) {
        this.groupId = groupId;
        this.title = title;
        this.description = description;
        this.tags = tags;
        this.stats = stats;
        this.settings = settings;
        this.userContext = userContext;
    }

    public Long getGroupId() { return groupId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public List<String> getTags() { return tags; }
    public Stats getStats() { return stats; }
    public Settings getSettings() { return settings; }
    public UserContext getUserContext() { return userContext; }

    public static class Stats {
        private final int memberCount;
        private final int problemCount;
        private final int contestCount;

        public Stats(int memberCount, int problemCount, int contestCount) {
            this.memberCount = memberCount;
            this.problemCount = problemCount;
            this.contestCount = contestCount;
        }

        public int getMemberCount() { return memberCount; }
        public int getProblemCount() { return problemCount; }
        public int getContestCount() { return contestCount; }
    }

    public static class Settings {
        private final boolean isPrivate;
        private final boolean isAutoApprove;

        public Settings(boolean isPrivate, boolean isAutoApprove) {
            this.isPrivate = isPrivate;
            this.isAutoApprove = isAutoApprove;
        }

        public boolean getIsPrivate() { return isPrivate; }
        public boolean getIsAutoApprove() { return isAutoApprove; }
    }

    public static class UserContext {
        private final String myStatus;
        private final boolean isLeader;

        public UserContext(String myStatus, boolean isLeader) {
            this.myStatus = myStatus;
            this.isLeader = isLeader;
        }

        public String getMyStatus() { return myStatus; }
        public boolean getIsLeader() { return isLeader; }
    }
}

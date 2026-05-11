package net.diveon.backend.domain.group.dto;

import java.util.List;

public class GroupListResponse {

    private long totalElements;
    private int totalPages;
    private int currentPage;
    private List<GroupItem> groups;

    public GroupListResponse(long totalElements, int totalPages, int currentPage, List<GroupItem> groups) {
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.currentPage = currentPage;
        this.groups = groups;
    }

    public long getTotalElements() { return totalElements; }
    public int getTotalPages() { return totalPages; }
    public int getCurrentPage() { return currentPage; }
    public List<GroupItem> getGroups() { return groups; }

    public static class GroupItem {
        private Long groupId;
        private String title;
        private String leader;
        private int memberCount;
        private int totalMembers;
        private List<String> tags;
        private boolean joined;

        public GroupItem(Long groupId, String title, String leader, int memberCount, int totalMembers, List<String> tags, boolean joined) {
            this.groupId = groupId;
            this.title = title;
            this.leader = leader;
            this.memberCount = memberCount;
            this.totalMembers = totalMembers;
            this.tags = tags;
            this.joined = joined;
        }

        public Long getGroupId() { return groupId; }
        public String getTitle() { return title; }
        public String getLeader() { return leader; }
        public int getMemberCount() { return memberCount; }
        public int getTotalMembers() { return totalMembers; }
        public List<String> getTags() { return tags; }
        public boolean isJoined() { return joined; }
    }
}

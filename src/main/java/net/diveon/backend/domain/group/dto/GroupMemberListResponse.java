package net.diveon.backend.domain.group.dto;

import java.time.LocalDateTime;
import java.util.List;

public class GroupMemberListResponse {

    private final long totalElements;
    private final int totalPages;
    private final int currentPage;
    private final List<Member> members;

    public GroupMemberListResponse(long totalElements, int totalPages, int currentPage, List<Member> members) {
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.currentPage = currentPage;
        this.members = members;
    }

    public long getTotalElements() { return totalElements; }
    public int getTotalPages() { return totalPages; }
    public int getCurrentPage() { return currentPage; }
    public List<Member> getMembers() { return members; }

    public static class Member {
        private final Long userId;
        private final String nickname;
        private final String role;
        private final LocalDateTime joinedAt;

        public Member(Long userId, String nickname, String role, LocalDateTime joinedAt) {
            this.userId = userId;
            this.nickname = nickname;
            this.role = role;
            this.joinedAt = joinedAt;
        }

        public Long getUserId() { return userId; }
        public String getNickname() { return nickname; }
        public String getRole() { return role; }
        public LocalDateTime getJoinedAt() { return joinedAt; }
    }
}

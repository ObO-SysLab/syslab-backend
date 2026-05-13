package net.diveon.backend.domain.group.dto;

import java.time.LocalDateTime;
import java.util.List;

public class GroupPendingMemberListResponse {

    private final long totalElements;
    private final int totalPages;
    private final int currentPage;
    private final List<PendingMember> pendingMembers;

    public GroupPendingMemberListResponse(long totalElements, int totalPages, int currentPage,
                                          List<PendingMember> pendingMembers) {
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.currentPage = currentPage;
        this.pendingMembers = pendingMembers;
    }

    public long getTotalElements() { return totalElements; }
    public int getTotalPages() { return totalPages; }
    public int getCurrentPage() { return currentPage; }
    public List<PendingMember> getPendingMembers() { return pendingMembers; }

    public static class PendingMember {
        private final Long userId;
        private final String nickname;
        private final LocalDateTime appliedAt;

        public PendingMember(Long userId, String nickname, LocalDateTime appliedAt) {
            this.userId = userId;
            this.nickname = nickname;
            this.appliedAt = appliedAt;
        }

        public Long getUserId() { return userId; }
        public String getNickname() { return nickname; }
        public LocalDateTime getAppliedAt() { return appliedAt; }
    }
}

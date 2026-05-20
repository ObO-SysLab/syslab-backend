package net.diveon.backend.domain.contest.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public class ContestParticipantListResponse {

    private final Long totalElements;
    private final Integer totalPages;
    private final List<ParticipantItem> participants;

    public ContestParticipantListResponse(Long totalElements, Integer totalPages,
                                          List<ParticipantItem> participants) {
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.participants = participants;
    }

    public Long getTotalElements() { return totalElements; }
    public Integer getTotalPages() { return totalPages; }
    public List<ParticipantItem> getParticipants() { return participants; }

    public static class ParticipantItem {
        private final Long userId;
        private final String nickname;
        private final LocalDateTime joinedAt;
        private final Integer score;
        private final Boolean isBanned;

        public ParticipantItem(Long userId, String nickname, LocalDateTime joinedAt,
                               Integer score, Boolean isBanned) {
            this.userId = userId;
            this.nickname = nickname;
            this.joinedAt = joinedAt;
            this.score = score;
            this.isBanned = isBanned;
        }

        public Long getUserId() { return userId; }
        public String getNickname() { return nickname; }
        public LocalDateTime getJoinedAt() { return joinedAt; }
        public Integer getScore() { return score; }
        public Boolean getIsBanned() { return isBanned; }
    }
}

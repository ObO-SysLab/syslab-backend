package net.diveon.backend.domain.activity.dto;

import java.time.LocalDateTime;
import java.util.List;

// 홈 '실시간 탐사 로그': 공개 문제의 최근 채점 결과
public class RecentActivityResponse {

    private final List<Item> activities;

    public RecentActivityResponse(List<Item> activities) {
        this.activities = activities;
    }

    public List<Item> getActivities() { return activities; }

    public static class Item {
        private final Long resultId;
        private final String resultStatus;
        private final LocalDateTime submittedAt;
        private final Long userId;
        private final String nickname;
        private final Long probId;
        private final String title;

        public Item(Long resultId, String resultStatus, LocalDateTime submittedAt,
                    Long userId, String nickname, Long probId, String title) {
            this.resultId = resultId;
            this.resultStatus = resultStatus;
            this.submittedAt = submittedAt;
            this.userId = userId;
            this.nickname = nickname;
            this.probId = probId;
            this.title = title;
        }

        public Long getResultId() { return resultId; }
        public String getResultStatus() { return resultStatus; }
        public LocalDateTime getSubmittedAt() { return submittedAt; }
        public Long getUserId() { return userId; }
        public String getNickname() { return nickname; }
        public Long getProbId() { return probId; }
        public String getTitle() { return title; }
    }
}

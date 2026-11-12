package net.diveon.backend.domain.user.dto;

import net.diveon.backend.domain.user.entity.User;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public class ProfileShowResponse {

    private final UserInfo userInfo;

    public ProfileShowResponse(UserInfo userInfo) {
        this.userInfo = userInfo;
    }

    public UserInfo getUserInfo() { return userInfo; }

    public static class UserInfo {
        private final Long userId;
        private final String nickname;
        private final String profileImgUrl;
        private final String selfComment;
        private final LocalDateTime createdAt;
        private final String email;
        private final String belong;
        private final List<String> interest;
        private final Integer tier;
        private final Integer score;

        public UserInfo(User user) {
            this.userId = user.getId();
            this.nickname = user.getNickname();
            this.profileImgUrl = user.getProfileImgUrl();
            this.selfComment = user.getComment();
            this.createdAt = user.getCreatedAt();
            this.email = user.getEmail();
            this.belong = user.getBelong();
            // 빈 관심 분야("")가 [""]로 내려가 빈 태그가 보이던 문제를 막는다.
            this.interest = user.getInterest() == null ? List.of()
                    : Arrays.stream(user.getInterest().split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
            this.tier = user.getTier();
            this.score = user.getScore();
        }

        public Long getUserId() { return userId; }

        public String getNickname() { return nickname; }
        public String getProfileImgUrl() { return profileImgUrl; }
        public String getSelfComment() { return selfComment; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public String getEmail() { return email; }
        public String getBelong() { return belong; }
        public List<String> getInterest() { return interest; }
        public Integer getTier() { return tier; }
        public Integer getScore() { return score; }
    }
}

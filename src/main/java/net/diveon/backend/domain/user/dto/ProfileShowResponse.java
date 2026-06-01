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
        private final String nickname;
        private final String profileImgUrl;
        private final String selfComment;
        private final LocalDateTime createdAt;
        private final String email;
        private final String belong;
        private final List<String> interest;

        public UserInfo(User user) {
            this.nickname = user.getNickname();
            this.profileImgUrl = user.getProfileImgUrl();
            this.selfComment = user.getComment();
            this.createdAt = user.getCreatedAt();
            this.email = user.getEmail();
            this.belong = user.getBelong();
            this.interest = user.getInterest() == null ? List.of() : Arrays.asList(user.getInterest().split(","));
        }

        public String getNickname() { return nickname; }
        public String getProfileImgUrl() { return profileImgUrl; }
        public String getSelfComment() { return selfComment; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public String getEmail() { return email; }
        public String getBelong() { return belong; }
        public List<String> getInterest() { return interest; }
    }
}

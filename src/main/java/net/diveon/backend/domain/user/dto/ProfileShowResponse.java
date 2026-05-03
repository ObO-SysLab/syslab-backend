package net.diveon.backend.domain.user.dto;

import net.diveon.backend.domain.user.entity.User;

import java.time.LocalDate;
import java.time.LocalDateTime;

// GET /api/profile/show 응답 DTO
// User 엔티티에서 필요한 정보만 골라서 클라이언트에 반환함
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
        private final String realName;
        private final LocalDate birthDate;
        private final LocalDateTime createdAt;
        private final String email;
        private final String phoneNumber;
        private final String belong;
        private final String interest;

        // User 엔티티를 응답 DTO로 변환
        public UserInfo(User user) {
            this.nickname = user.getNickname();
            this.profileImgUrl = user.getProfileImgUrl();
            this.selfComment = user.getComment();
            this.realName = user.getRealName();
            this.birthDate = user.getBirthday();
            this.createdAt = user.getCreatedAt();
            this.email = user.getEmail();
            this.phoneNumber = user.getPhoneNumber();
            this.belong = user.getBelong();
            this.interest = user.getInterest();
        }

        public String getNickname() { return nickname; }
        public String getProfileImgUrl() { return profileImgUrl; }
        public String getSelfComment() { return selfComment; }
        public String getRealName() { return realName; }
        public LocalDate getBirthDate() { return birthDate; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public String getEmail() { return email; }
        public String getPhoneNumber() { return phoneNumber; }
        public String getBelong() { return belong; }
        public String getInterest() { return interest; }
    }
}

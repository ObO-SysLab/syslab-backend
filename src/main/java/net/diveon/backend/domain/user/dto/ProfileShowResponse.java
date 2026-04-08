package net.diveon.backend.domain.user.dto;

import net.diveon.backend.domain.user.entity.User;

import java.time.LocalDateTime;
import java.time.LocalTime;

// GET /api/profile/show 응답 DTO
// User 엔티티에서 필요한 정보만 골라서 클라이언트에 반환함
public class ProfileShowResponse {

    private final UserInfo user_info;

    public ProfileShowResponse(UserInfo user_info) {
        this.user_info = user_info;
    }

    public UserInfo getUser_info() { return user_info; }

    public static class UserInfo {
        private final String nick_name;
        private final String profile_img_url;
        private final String self_comment;
        private final String real_name;
        private final LocalTime birth_date;
        private final LocalDateTime created_at;
        private final String email;
        private final String phone_number;
        private final String belong;
        private final String interest;

        // User 엔티티를 응답 DTO로 변환
        public UserInfo(User user) {
            this.nick_name = user.getNickName();
            this.profile_img_url = user.getProfileUrl();
            this.self_comment = user.getComment();
            this.real_name = user.getRealName();
            this.birth_date = user.getBirthday();
            this.created_at = user.getCreatedAt();
            this.email = user.getEmail();
            this.phone_number = user.getPhoneNumber();
            this.belong = user.getBelong();
            this.interest = user.getInterest();
        }

        public String getNick_name() { return nick_name; }
        public String getProfile_img_url() { return profile_img_url; }
        public String getSelf_comment() { return self_comment; }
        public String getReal_name() { return real_name; }
        public LocalTime getBirth_date() { return birth_date; }
        public LocalDateTime getCreated_at() { return created_at; }
        public String getEmail() { return email; }
        public String getPhone_number() { return phone_number; }
        public String getBelong() { return belong; }
        public String getInterest() { return interest; }
    }
}

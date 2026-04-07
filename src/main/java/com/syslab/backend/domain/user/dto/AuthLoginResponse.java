package com.syslab.backend.domain.user.dto;

public class AuthLoginResponse {

    private final String access_token;
    private final String refresh_token;
    private final UserInfo user_info;

    public AuthLoginResponse(String access_token, String refresh_token, UserInfo user_info) {
        this.access_token = access_token;
        this.refresh_token = refresh_token;
        this.user_info = user_info;
    }

    public String getAccess_token() { return access_token; }
    public String getRefresh_token() { return refresh_token; }
    public UserInfo getUser_info() { return user_info; }

    public static class UserInfo {
        private final String nickname;
        private final String profile_img_url;

        public UserInfo(String nickname, String profile_img_url) {
            this.nickname = nickname;
            this.profile_img_url = profile_img_url;
        }

        public String getNickname() { return nickname; }
        public String getProfile_img_url() { return profile_img_url; }
    }
}

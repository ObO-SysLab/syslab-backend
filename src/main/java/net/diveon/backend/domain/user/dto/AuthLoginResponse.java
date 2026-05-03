package net.diveon.backend.domain.user.dto;

public class AuthLoginResponse {

    private final String accessToken;
    private final String refreshToken;
    private final UserInfo userInfo;

    public AuthLoginResponse(String accessToken, String refreshToken, UserInfo userInfo) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.userInfo = userInfo;
    }

    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public UserInfo getUserInfo() { return userInfo; }

    public static class UserInfo {
        private final String nickname;
        private final String profileImgUrl;

        public UserInfo(String nickname, String profileImgUrl) {
            this.nickname = nickname;
            this.profileImgUrl = profileImgUrl;
        }

        public String getNickname() { return nickname; }
        public String getProfileImgUrl() { return profileImgUrl; }
    }
}

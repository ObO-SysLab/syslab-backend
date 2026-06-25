package net.diveon.backend.domain.user.dto;

public interface UserRankingProjection {
    Integer getRank();
    Long getUserId();
    String getNickname();
    String getProfileImgUrl();
    Integer getTier();
    Integer getScore();
}

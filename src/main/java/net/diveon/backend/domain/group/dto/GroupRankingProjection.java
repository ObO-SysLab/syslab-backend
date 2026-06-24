package net.diveon.backend.domain.group.dto;

public interface GroupRankingProjection {
    Long getGroupId();
    String getTitle();
    String getImage();
    Long getMemberCount();
    Long getScore();
}

package net.diveon.backend.domain.user.dto;

import java.util.List;

public class ProfileUpdateRequest {

    private String nickname;
    private String selfComment;
    private String belong;
    private List<String> interest;

    public String getNickname() { return nickname; }
    public String getSelfComment() { return selfComment; }
    public String getBelong() { return belong; }
    public List<String> getInterest() { return interest; }
}

package net.diveon.backend.domain.user.dto;

import java.util.List;

public class ProfileUpdateRequest {

    private String nickname;
    private String selfComment;
    private String email;
    private String phoneNumber;
    private String belong;
    private List<String> interest;

    public String getNickname() { return nickname; }
    public String getSelfComment() { return selfComment; }
    public String getEmail() { return email; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getBelong() { return belong; }
    public List<String> getInterest() { return interest; }
}

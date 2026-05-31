package net.diveon.backend.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public class AuthSignUpRequest {
    @NotBlank
    @Email
    @JsonProperty("email")
    private String email;

    @NotBlank
    @JsonProperty("loginId")
    private String loginId;

    @NotBlank
    @JsonProperty("password")
    private String password;

    @NotBlank
    @JsonProperty("nickname")
    private String nickname;

    @JsonProperty("belong")
    private String belong;

    @JsonProperty("interest")
    private List<String> interest;

    public AuthSignUpRequest() {}

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getLoginId() { return loginId; }
    public void setLoginId(String loginId) { this.loginId = loginId; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getBelong() { return belong; }
    public void setBelong(String belong) { this.belong = belong; }

    public List<String> getInterest() { return interest; }
    public void setInterest(List<String> interest) { this.interest = interest; }
}

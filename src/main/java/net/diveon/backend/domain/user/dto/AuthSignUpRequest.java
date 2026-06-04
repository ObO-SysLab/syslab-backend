package net.diveon.backend.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

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
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?])[^\\s]{8,16}$",
             message = "비밀번호는 8~16자이며, 영문, 숫자, 특수문자를 각각 1개 이상 포함해야 합니다.")
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

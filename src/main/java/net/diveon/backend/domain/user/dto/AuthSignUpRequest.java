package net.diveon.backend.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;



/**
 * <pre>
 * 회원 가입 요청시 사용할 api 요청의 body의 JSON에 대응하기 위한 dto 클래스.
 * 현재 회원 가입 기능의 request body는 다음과 같음
 * {
    "email": "{{userEmailAddrInput}}",
    "user_id": "{{userIdInput}}",
    "password": "{{userPwInput}}",
    "nick_name": "{{userNicknameInpu}}",
    "belong": "{{userBelongInput}}", 
    "interest": "{{userInterestInput}}" 
    }
 * </pre>
 */
public class AuthSignUpRequest {
    @JsonProperty("email")
    private String email;

    @NotBlank
    @JsonProperty("user_id")
    private String userId;

    @NotBlank
    @JsonProperty("password")
    private String password;

    @NotBlank
    @JsonProperty("nick_name")
    private String nickName;

    @JsonProperty("belong")
    private String belong;

    @JsonProperty("interest")
    private String interest;

    // 수정사항, JavaBeans 표준규격에 맞도록, 생성자, getter와 setter 수정
    // Default constructor for JavaBeans and Jackson
    public AuthSignUpRequest() {}

    // Getters and Setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getBelong() {
        return belong;
    }

    public void setBelong(String belong) {
        this.belong = belong;
    }

    public String getInterest() {
        return interest;
    }

    public void setInterest(String interest) {
        this.interest = interest;
    }
}

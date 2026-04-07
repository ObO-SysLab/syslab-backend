package com.syslab.backend.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;



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
    "interset": "{{userInterstInput}}" 
    }
 * </pre>
 */
public class AuthSignUpRequest {
    //현재는 api 통신시, json의 키 값과, 사용하지 않는 변수명에 대해서만 @JsonProperty 어노테이션을 부여하였음.
    //
    @JsonProperty("email")
    private String email;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("password")
    private String password;

    @JsonProperty("nick_name")
    private String nickName;

    @JsonProperty("belong")
    private String belong;

    @JsonProperty("interest")
    private String interest;


    // 아래는 데이터를 확인하기 위한 getter 메서드 들
    public String getUserId(){
        return this.userId;
    }
    public String getUserPassword(){
        return this.password;
    }
    public String getUserNickName(){
        return this.nickName;
    }
    public String getUserBelong(){
        return this.belong;
    }
    public String getUserInterest(){
        return this.interest;
    }
    public String getUserEmail(){
        return this.email;
    }

}

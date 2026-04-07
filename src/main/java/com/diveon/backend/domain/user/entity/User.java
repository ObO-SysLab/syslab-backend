package com.diveon.backend.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.time.LocalTime;

// domain_user 테이블과 매핑되는 엔티티
@Entity
@Table(name = "domain_user")
public class User {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "real_name")
    private String realName;

    @Column(name = "nick_name", nullable = false)
    private String nickName;

    @Column(name = "profile_url")
    private String profileUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "birthday")
    private LocalTime birthday;

    @Column(name = "email")
    private String email;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "comment")
    private String comment;

    @Column(name = "tier", nullable = false)
    private Short tier;

    @Column(name = "sex", length = 1)
    private String sex;

    @Column(name = "belong")
    private String belong;

    @Column(name = "interest")
    private String interest;

    //수정 시작 - 안상완 2026.04.07 - 
    //유저 정보를 레포지토리에 저장하기 위한 새로운 생성자
    // 및 기존 JPA를 이용하는 코드를 유지하기 위한 기본 생성자 생성
    //JPA 의존성을 이용하기 위한, 기본 생성자
    public User() {};
    
    // 회원가입용 생성자
    //
    public User(String id, String password, String nickName, String email, String belong, String interest) {
        this.id = id;
        this.password = password; // 주의: 실제 서비스에선 암호화 필수!
        this.nickName = nickName;
        this.email = email;
        this.belong = belong;
        this.interest = interest;
        this.createdAt = LocalDateTime.now(); // 가입 시간 설정
        this.tier = 1; // 기본 티어 설정
    }
     //수정 종료 - 안상완 2026.04.07 - 
     
    public String getId() { return id; }
    public String getPassword() { return password; }
    public String getRealName() { return realName; }
    public String getNickName() { return nickName; }
    public String getProfileUrl() { return profileUrl; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalTime getBirthday() { return birthday; }
    public String getEmail() { return email; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getComment() { return comment; }
    public Short getTier() { return tier; }
    public String getSex() { return sex; }
    public String getBelong() { return belong; }
    public String getInterest() { return interest; }
}

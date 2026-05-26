package net.diveon.backend.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "domain_user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "login_id", unique = true, nullable = false, length = 50)
    private String loginId;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "real_name")
    private String realName;

    @Column(name = "nickname", unique = true, nullable = false, length = 50)
    private String nickname;

    @Column(name = "profile_img_url", length = 500)
    private String profileImgUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "birthday")
    private LocalDate birthday;

    @Column(name = "email")
    private String email;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "comment")
    private String comment;

    @Column(name = "tier", nullable = false)
    private Integer tier;

    @Column(name = "score", nullable = false)
    private Integer score;

    @Column(name = "sex", length = 1)
    private String sex;

    @Column(name = "belong")
    private String belong;

    @Column(name = "interest")
    private String interest;

    public User() {}

    public User(String loginId, String password, String nickname, String email, String belong, List<String> interest) {
        this.loginId = loginId;
        this.password = password;
        this.nickname = nickname;
        this.email = email;
        this.belong = belong;
        this.interest = interest == null ? null : String.join(",", interest);
        this.createdAt = LocalDateTime.now();
        this.tier = 0;
        this.score = 0;
    }

    public void updateProfile(String nickname, String comment, String email, String phoneNumber, String belong, List<String> interest) {
        if (nickname != null) this.nickname = nickname;
        if (comment != null) this.comment = comment;
        if (email != null) this.email = email;
        if (phoneNumber != null) this.phoneNumber = phoneNumber;
        if (belong != null) this.belong = belong;
        if (interest != null) this.interest = String.join(",", interest);
    }

    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public Long getId() { return id; }
    public String getLoginId() { return loginId; }
    public String getPassword() { return password; }
    public String getRealName() { return realName; }
    public String getNickname() { return nickname; }
    public String getProfileImgUrl() { return profileImgUrl; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDate getBirthday() { return birthday; }
    public String getEmail() { return email; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getComment() { return comment; }
    public Integer getTier() { return tier; }
    public Integer getScore() { return score; }
    public String getSex() { return sex; }
    public String getBelong() { return belong; }
    public String getInterest() { return interest; }
}

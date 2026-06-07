package net.diveon.backend.domain.group.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import net.diveon.backend.domain.user.entity.User;

@Entity
@Table(name = "domain_group")
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leader_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User leader;

    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(name = "limit_member_count", nullable = false, columnDefinition = "SMALLINT DEFAULT 50")
    private Short limitMemberCount;

    @Column(name = "image", columnDefinition = "TEXT")
    private String image;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_private", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isPrivate;

    @Column(name = "is_auto_approve", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isAutoApprove;

    @Column(name = "invitation_code", unique = true, length = 50)
    private String invitationCode;

    public Group() {
    }

    public Group(User leader, Short limitMemberCount, String image, String title, String description,
                 Boolean isPrivate, Boolean isAutoApprove, String invitationCode) {
        this.leader = leader;
        this.createdAt = LocalDateTime.now();
        this.limitMemberCount = limitMemberCount != null ? limitMemberCount : 50;
        this.image = image;
        this.title = title;
        this.description = description;
        this.isPrivate = isPrivate != null ? isPrivate : false;
        this.isAutoApprove = isAutoApprove != null ? isAutoApprove : false;
        this.invitationCode = invitationCode;
    }

    public Long getId() { return id; }
    public User getLeader() { return leader; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Short getLimitMemberCount() { return limitMemberCount; }
    public String getImage() { return image; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Boolean getIsPrivate() { return isPrivate; }
    public Boolean getIsAutoApprove() { return isAutoApprove; }
    public String getInvitationCode() { return invitationCode; }

    public void update(String title, String description, Boolean isPrivate, Boolean isAutoApprove) {
        this.title = title;
        this.description = description;
        this.isPrivate = isPrivate;
        this.isAutoApprove = isAutoApprove;
    }

    public void updateImage(String image) {
        this.image = image;
    }
}

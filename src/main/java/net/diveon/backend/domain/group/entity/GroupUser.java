package net.diveon.backend.domain.group.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import net.diveon.backend.domain.user.entity.User;

@Entity
@Table(
    name = "group_user",
    uniqueConstraints = {
        @UniqueConstraint(name = "UK_GROUP_USER_UNIQUE", columnNames = {"group_id", "user_id"})
    }
)
public class GroupUser {

    public static enum GroupRole {
        LEADER,
        MEMBER
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 10)
    private GroupRole role;

    @Column(name = "joined_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime joinedAt;

    public GroupUser() {
    }

    public GroupUser(Group group, User user, GroupRole role) {
        this.group = group;
        this.user = user;
        this.role = role;
        this.joinedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Group getGroup() { return group; }
    public User getUser() { return user; }
    public GroupRole getRole() { return role; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
}

package net.diveon.backend.domain.contest.entity;

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
    name = "contest_participant",
    uniqueConstraints = {
        @UniqueConstraint(name = "UK_CONTEST_PARTICIPANT_UNIQUE", columnNames = {"contest_id", "user_id"})
    }
)
public class ContestParticipant {

    public enum ContestRole {
        ADMIN, PARTICIPANT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contest_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Contest contest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private ContestRole role;

    @Column(name = "joined_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime joinedAt;

    @Column(name = "is_banned", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isBanned = false;

    @Column(name = "score", nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer score = 0;

    @Column(name = "last_solved_at", columnDefinition = "TIMESTAMP")
    private LocalDateTime lastSolvedAt;

    public ContestParticipant() {}

    public ContestParticipant(Contest contest, User user, ContestRole role) {
        this.contest = contest;
        this.user = user;
        this.role = role;
        this.joinedAt = LocalDateTime.now();
        this.isBanned = false;
        this.score = 0;
    }

    public Long getId() { return id; }
    public Contest getContest() { return contest; }
    public User getUser() { return user; }
    public ContestRole getRole() { return role; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
    public Boolean getIsBanned() { return isBanned; }
    public Integer getScore() { return score; }
    public LocalDateTime getLastSolvedAt() { return lastSolvedAt; }

    public void ban() { this.isBanned = true; }
    public void unban() { this.isBanned = false; }

    public void updateRole(ContestRole role) {
        this.role = role;
    }

    public void addScore(Integer points, LocalDateTime solvedAt) {
        this.score += points;
        this.lastSolvedAt = solvedAt;
    }
}

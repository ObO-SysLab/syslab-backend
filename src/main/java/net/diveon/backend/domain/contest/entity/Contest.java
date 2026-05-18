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
import net.diveon.backend.domain.group.entity.Group;
import net.diveon.backend.domain.user.entity.User;

@Entity
@Table(name = "contest")
public class Contest {

    public enum ContestType {
        OFFICIAL, GROUP, CLASS, INDIVIDUAL
    }

    public enum ParticipationType {
        INDIVIDUAL, TEAM
    }

    public enum Visibility {
        PUBLIC, GROUP
    }

    public enum ContestStatus {
        UPCOMING, ONGOING, ENDED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Group group;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "contest_type", nullable = false, length = 20)
    private ContestType contestType;

    @Enumerated(EnumType.STRING)
    @Column(name = "participation_type", nullable = false, length = 20)
    private ParticipationType participationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 20)
    private Visibility visibility;

    @Column(name = "start_time", nullable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime endTime;

    @Column(name = "rules", columnDefinition = "TEXT")
    private String rules;

    @Column(name = "prize_description", length = 500)
    private String prizeDescription;

    @Column(name = "is_hot", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isHot = false;

    @Column(name = "poster_url", length = 500)
    private String posterUrl;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime updatedAt;

    public Contest() {}

    public Contest(User createdBy, Group group, String title, String description,
                   ContestType contestType, ParticipationType participationType, Visibility visibility,
                   LocalDateTime startTime, LocalDateTime endTime,
                   String rules, String prizeDescription, String posterUrl) {
        this.createdBy = createdBy;
        this.group = group;
        this.title = title;
        this.description = description;
        this.contestType = contestType;
        this.participationType = participationType;
        this.visibility = visibility;
        this.startTime = startTime;
        this.endTime = endTime;
        this.rules = rules;
        this.prizeDescription = prizeDescription;
        this.posterUrl = posterUrl;
        this.isHot = false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public ContestStatus getStatus() {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(startTime)) return ContestStatus.UPCOMING;
        if (now.isAfter(endTime)) return ContestStatus.ENDED;
        return ContestStatus.ONGOING;
    }

    public Long getId() { return id; }
    public User getCreatedBy() { return createdBy; }
    public Group getGroup() { return group; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public ContestType getContestType() { return contestType; }
    public ParticipationType getParticipationType() { return participationType; }
    public Visibility getVisibility() { return visibility; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public String getRules() { return rules; }
    public String getPrizeDescription() { return prizeDescription; }
    public Boolean getIsHot() { return isHot; }
    public String getPosterUrl() { return posterUrl; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void update(String title, String description, ContestType contestType,
                       ParticipationType participationType, Visibility visibility,
                       LocalDateTime startTime, LocalDateTime endTime,
                       String rules, String prizeDescription, String posterUrl) {
        this.title = title;
        this.description = description;
        this.contestType = contestType;
        this.participationType = participationType;
        this.visibility = visibility;
        this.startTime = startTime;
        this.endTime = endTime;
        this.rules = rules;
        this.prizeDescription = prizeDescription;
        this.posterUrl = posterUrl;
        this.updatedAt = LocalDateTime.now();
    }
}

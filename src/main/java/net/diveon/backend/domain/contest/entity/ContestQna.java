package net.diveon.backend.domain.contest.entity;

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
@Table(name = "contest_qna")
public class ContestQna {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contest_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Contest contest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "questioner_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User questioner;

    @Column(name = "question", nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(name = "answer", columnDefinition = "TEXT")
    private String answer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answered_by")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private User answeredBy;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(name = "answered_at", columnDefinition = "TIMESTAMP")
    private LocalDateTime answeredAt;

    public ContestQna() {}

    public ContestQna(Contest contest, User questioner, String question) {
        this.contest = contest;
        this.questioner = questioner;
        this.question = question;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Contest getContest() { return contest; }
    public User getQuestioner() { return questioner; }
    public String getQuestion() { return question; }
    public String getAnswer() { return answer; }
    public User getAnsweredBy() { return answeredBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getAnsweredAt() { return answeredAt; }

    public void answer(User answeredBy, String answer) {
        this.answeredBy = answeredBy;
        this.answer = answer;
        this.answeredAt = LocalDateTime.now();
    }
}

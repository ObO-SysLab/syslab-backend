package net.diveon.backend.domain.contest.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
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

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "qna", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("answeredAt ASC")
    private List<ContestQnaAnswer> answers = new ArrayList<>();

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
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<ContestQnaAnswer> getAnswers() { return answers; }
}

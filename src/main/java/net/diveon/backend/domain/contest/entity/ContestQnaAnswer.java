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
@Table(name = "contest_qna_answer")
public class ContestQnaAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "qna_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ContestQna qna;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answered_by", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User answeredBy;

    @Column(name = "answer", nullable = false, columnDefinition = "TEXT")
    private String answer;

    @Column(name = "answered_at", nullable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime answeredAt;

    public ContestQnaAnswer() {}

    public ContestQnaAnswer(ContestQna qna, User answeredBy, String answer) {
        this.qna = qna;
        this.answeredBy = answeredBy;
        this.answer = answer;
        this.answeredAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public ContestQna getQna() { return qna; }
    public User getAnsweredBy() { return answeredBy; }
    public String getAnswer() { return answer; }
    public LocalDateTime getAnsweredAt() { return answeredAt; }
}

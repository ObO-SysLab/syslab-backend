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
import net.diveon.backend.domain.grade.entity.SolveSubmission;
import net.diveon.backend.domain.user.entity.User;

@Entity
@Table(name = "contest_submission")
public class ContestSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contest_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Contest contest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contest_problem_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ContestProblem contestProblem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private SolveSubmission solveSubmission;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Column(name = "submitted_at", nullable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime submittedAt;

    public ContestSubmission() {}

    public ContestSubmission(Contest contest, ContestProblem contestProblem, User user, SolveSubmission solveSubmission) {
        this.contest = contest;
        this.contestProblem = contestProblem;
        this.user = user;
        this.solveSubmission = solveSubmission;
        this.isCorrect = null;
        this.submittedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Contest getContest() { return contest; }
    public ContestProblem getContestProblem() { return contestProblem; }
    public User getUser() { return user; }
    public SolveSubmission getSolveSubmission() { return solveSubmission; }
    public Boolean getIsCorrect() { return isCorrect; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }

    public void updateResult(Boolean isCorrect) {
        this.isCorrect = isCorrect;
    }
}

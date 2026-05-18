package net.diveon.backend.domain.contest.entity;

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
import jakarta.persistence.UniqueConstraint;
import net.diveon.backend.domain.problem.entity.Problem;

@Entity
@Table(
    name = "contest_problem",
    uniqueConstraints = {
        @UniqueConstraint(name = "UK_CONTEST_PROBLEM_UNIQUE", columnNames = {"contest_id", "problem_id"})
    }
)
public class ContestProblem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contest_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Contest contest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Problem problem;

    @Column(name = "points", nullable = false)
    private Integer points;

    public ContestProblem() {}

    public ContestProblem(Contest contest, Problem problem, Integer points) {
        this.contest = contest;
        this.problem = problem;
        this.points = points;
    }

    public Long getId() { return id; }
    public Contest getContest() { return contest; }
    public Problem getProblem() { return problem; }
    public Integer getPoints() { return points; }

    public void updatePoints(Integer points) {
        this.points = points;
    }
}

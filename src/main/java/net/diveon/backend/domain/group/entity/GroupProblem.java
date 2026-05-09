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
import jakarta.persistence.UniqueConstraint;
import net.diveon.backend.domain.problem.entity.Problem;

@Entity
@Table(
    name = "group_problem",
    uniqueConstraints = {
        @UniqueConstraint(name = "UK_GROUP_PROBLEM_UNIQUE", columnNames = {"group_id", "problem_id"})
    }
)
public class GroupProblem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Problem problem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Group group;

    @Column(name = "assigned_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime assignedAt;

    public GroupProblem() {
    }

    public GroupProblem(Problem problem, Group group) {
        this.problem = problem;
        this.group = group;
        this.assignedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Problem getProblem() { return problem; }
    public Group getGroup() { return group; }
    public LocalDateTime getAssignedAt() { return assignedAt; }
}

package net.diveon.backend.domain.problem.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "obo_steps")
public class OboStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prob_id", nullable = false)
    private Problem problem;

    @Column(name = "step", nullable = false)
    private Integer step;

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;

    // 1. JPA용 기본 생성자
    public OboStep() {
    }

    // 2. 데이터 생성을 위한 생성자
    public OboStep(Problem problem, Integer step, String description) {
        this.problem = problem;
        this.step = step;
        this.description = description;
    }

    // 3. Getter 메서드
    public Long getId() { return id; }
    public Problem getProblem() { return problem; }
    public Integer getStep() { return step; }
    public String getDescription() { return description; }

    // 4. Setter 메서드
    public void setProblem(Problem problem) { this.problem = problem; }
    public void setStep(Integer step) { this.step = step; }
    public void setDescription(String description) { this.description = description; }
}

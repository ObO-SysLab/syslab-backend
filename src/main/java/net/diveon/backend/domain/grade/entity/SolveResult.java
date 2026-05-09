package net.diveon.backend.domain.grade.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;

import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
/**
 * <pre>
 * | 컬럼            | 타입        | 제약                           | 설명              |
| ------------- | --------- | ---------------------------- | --------------- |
| id            | BIGINT    | PK, AUTO_INCREMENT           | PK용             |
| submisson_id  | BIGINT    | FK → submission.id, NOT NULL |                 |
| result_status | enum      | NOT NULL                     | 클래스 enum 참고     |
| created_at    | TIMESTAMP | NOT NULL, DEFAULT NOW()      | 채점 완료 후 기록시간    |
 * </pre>
 */
@Entity
public class SolveResult {
    public static enum SolveResultState{
        CORRECT,
        WRONG,
        ERROR
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false)
    private SolveSubmission submission;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_status", nullable = false)
    private SolveResultState resultState;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime createdAt = LocalDateTime.now();

    //jpa용
    public SolveResult() {}


    public SolveResult(SolveSubmission submission, SolveResultState resultState) {
        this.submission = submission;
        this.resultState = resultState;
        this.createdAt = java.time.LocalDateTime.now();
    }

    public Long getId() { return id; }
    public SolveSubmission getSubmission() { return submission; }
    public SolveResultState getResultState() { return resultState; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}

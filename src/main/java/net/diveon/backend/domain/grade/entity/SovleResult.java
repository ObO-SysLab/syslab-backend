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
| is_passed     | enum      | NOT NULL                     | 클래스 enum 참고     |
| message       | String    |                              |                 |
| created_at    | TIMESTAMP | NOT NULL, DEFAULT NOW()      | 채점 완료 후 기록시간    |
 * </pre>
 */
@Entity
public class SovleResult {
    public static enum SovleResultState{
        NOGRADE,
        CORRECT,
        WRONG,
        ERROR,
        ACCEPTED,
        WRONG_ANSWER,
        TIME_LIMIT_EXCEEDED,
        RUNTIME_ERROR,
        COMPILE_ERROR,
        SYSTEM_ERROR
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false)
    private SolveSubmission submission;

    @Enumerated(EnumType.STRING)
    @Column(name = "is_passed", nullable = false)
    private SovleResultState resultState = SovleResultState.NOGRADE;

    @Column(name = "message")
    private String message;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime createdAt = LocalDateTime.now();

    //jpa용
    public SovleResult() {}


    public SovleResult(SolveSubmission submission, SovleResultState resultState, String message) {
        this.submission = submission;
        this.resultState = resultState;
        this.message = message;
        this.createdAt = java.time.LocalDateTime.now();
    }

    public Long getId() { return id; }
    public SolveSubmission getSubmission() { return submission; }
    public SovleResultState getResultState() { return resultState; }
    public String getMessage() { return message; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}

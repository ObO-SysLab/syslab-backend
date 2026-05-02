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
| login_id      | BIGINT    | FK → users.id, NOT NULL      | 이 컬럼은 고민이 좀더 필요 |
| is_passed     | enum      | NOT NULL                     | 클래스 enum 참고     |
| socre         | SMALLINT  |                              |                 |
| memory_useage | SMALLINT  |                              | 총 100점          |
| runtime       | SMALLINT  |                              | 총 100점          |
| code_size     | SMALLINT  |                              | 총 100점          |
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
        ERROR
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

    @Column(name = "score")
    private Short score = 0;

    @Column(name = "memory_usage")
    private Short memoryUsage = 0;

    @Column(name = "runtime")
    private Short runtime = 0;

    @Column(name = "code_size")
    private Short codeSize = 0;

    @Column(name = "message")
    private String message;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime createdAt = LocalDateTime.now();

    //jpa용
    public SovleResult() {}


    public SovleResult(SolveSubmission submission, SovleResultState resultState, String message) {
        this.submission = submission;
        this.resultState = resultState;
        // this.score = score;
        // this.memoryUsage = memoryUsage;
        // this.runtime = runtime;
        // this.codeSize = codeSize;
        this.message = message;
        this.createdAt = java.time.LocalDateTime.now();
    }

    public Long getId() { return id; }
    public SolveSubmission getSubmission() { return submission; }
    public SovleResultState getResultState() { return resultState; }
    public Short getScore() { return score; }
    public Short getMemoryUsage() { return memoryUsage; }
    public Short getRuntime() { return runtime; }
    public Short getCodeSize() { return codeSize; }
    public String getMessage() { return message; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}

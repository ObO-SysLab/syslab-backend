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
import jakarta.persistence.ManyToOne;
import net.diveon.backend.domain.problem.entity.Problem;
import net.diveon.backend.domain.user.entity.User;

/**
 * <pre>
 * | 컬럼           | 타입        | 제약                                         | 설명          |
| ------------ | --------- | ------------------------------------------ | ----------- |
| id           | BIGINT    | PK, AUTO_INCREMENT                         |             |
| prob_id      | BIGINT    | FK → problem_summary.id, NOT NULL          |             |
| submitter_id | BIGINT    | FK → users.id, NOT NULL |             |
| result       | enum      | NOT NULL                                   | 클래스 enum 참고 |
| submitted_at | TIMESTAMP | NOT NULL, DEFAULT NOW()                    | 제출 시각       |
</pre>
 */
@Entity
public class SolveSubmission {

    //문자열 줄때는 .name() 으로 줄 것
    public static enum SubmissionState{
        PENDING,
        RECEIVED,
        INPROGRESS,
        COMPLETED,
        FAILED,
        TIMEOUT,
        ERROR,
        UNKNOWN
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;


    // private long prob_id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prob_id", nullable =  false)
    private Problem problem;

    // private long submitter_id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitter_id", nullable =  false)
    private User user;


    // private String submission_state;
    @Enumerated(EnumType.STRING)
    @Column(name = "submission_state", nullable = false)
    private SubmissionState submissionState = SubmissionState.PENDING;

    // private LocalDateTime submitted_at;
    @Column(name  = "submitted_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime submittedAt = LocalDateTime.now();

    //JPA용 생성자
    public SolveSubmission(){}


    /**
     * <pre>
     * 채점 요청용 생성자
     * SubmissionStae 는 PENDING으로
     * submittedAt은 LocalDateTime.now()로 
     * 자동설정된다.
     * 
     * </pre>
     * @param problem
     * @param user
     */
    public SolveSubmission(Problem problem, User user){
        this.problem = problem;
        this.user = user;
        this.submissionState = SubmissionState.PENDING;
        this.submittedAt = LocalDateTime.now();
    }

    //아래는 Getter들

    public long getId(){
        return id;
    }

    public Problem getProblem(){
        return problem;
    }

    public User getUser(){
        return user;
    }

    // enum 타입으로 반환하는것 명심
    public SubmissionState getSubmissionState(){
        return submissionState; 
    }

    public LocalDateTime getSubmittedAt(){
        return submittedAt;
    }
}

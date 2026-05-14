package net.diveon.backend.domain.grade.entity;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;;

@Entity
public class SolveSubmissionObjective {

    //@GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id")
    private long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "submission_id", nullable = false)
    // 현재는 DB레벨에서 처리하지만 다음과같이 JPA에서 처리하도록 수정이 가능함. 차후 비교를 통해 변경할 것이라면 논의가 필요하다.
    @OnDelete(action = OnDeleteAction.CASCADE)
    private SolveSubmission submission;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "answer", columnDefinition = "JSONB", nullable = false)
    private List<Integer> answer;

    public SolveSubmissionObjective() {}

    public SolveSubmissionObjective(SolveSubmission submission,List<Integer> userAnswer) {
        this.submission = submission;
        this.answer = userAnswer;
    }

    public List<Integer> getAnswer() {
        return answer;
    }
    public long getId() {
        return id;
    }
    public SolveSubmission getSubmission() {
        return submission;
    }
}

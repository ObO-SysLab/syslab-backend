package net.diveon.backend.domain.grade.entity;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;

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

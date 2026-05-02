package net.diveon.backend.domain.grade.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;

@Entity
public class SolveSubmissionPractice {
    @Id
    @Column(name = "id")
    private long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "submission_id", nullable = false)
    private SolveSubmission submission;

    @Column(name = "answer", columnDefinition = "TEXT", nullable =  false)
    private String answer;

    public SolveSubmissionPractice(){}
    public SolveSubmissionPractice(SolveSubmission submission, String userAnswer) {
        this.submission = submission;
        this.answer = userAnswer;
    }
    public String getAnswer() {
        return answer;
    }
    public long getId() {
        return id;
    }
    public SolveSubmission getSubmission() {
        return submission;
    }
}   

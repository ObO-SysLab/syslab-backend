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

    //현재 실습형은 vm과 관련된 내용이 협의 되지 않아 아직삭제를 한번에 삭제 구현 안했습니다.
    //다만 submission 자체에는 user 삭제되면 cascade 걸어놔서 오류 메세지 출력될겁니다.
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

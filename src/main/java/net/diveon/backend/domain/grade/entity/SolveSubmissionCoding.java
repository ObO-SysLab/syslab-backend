package net.diveon.backend.domain.grade.entity;



import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;

@Entity
public class SolveSubmissionCoding {

    @Id
    @Column(name = "id")
    private long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "submission_id", nullable = false)
    private SolveSubmission submission;

    @Column(name = "answer", nullable =  false, columnDefinition = "TEXT")
    private String answer;

    @Column(name = "language", nullable = false, length = 20)
    private String language;

    public SolveSubmissionCoding () {}
    public SolveSubmissionCoding (SolveSubmission submission, String userAnswer, String language) {
        this.submission = submission;
        this.answer = userAnswer;
        this.language = language;
    }
    public String getAnswer() {
        return answer;
    }
    public String getLanguage() {
        return language;
    }
    public long getId() {
        return id;
    }
    public SolveSubmission getSubmission() {
        return submission;
    }
}

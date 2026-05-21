package net.diveon.backend.domain.contest.entity;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "contest_tag",
    uniqueConstraints = {
        @UniqueConstraint(name = "UK_CONTEST_TAG_UNIQUE", columnNames = {"contest_id", "tag"})
    }
)
public class ContestTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contest_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Contest contest;

    @Column(name = "tag", nullable = false, length = 50)
    private String tag;

    public ContestTag() {}

    public ContestTag(Contest contest, String tag) {
        this.contest = contest;
        this.tag = tag;
    }

    public Long getId() { return id; }
    public Contest getContest() { return contest; }
    public String getTag() { return tag; }
}

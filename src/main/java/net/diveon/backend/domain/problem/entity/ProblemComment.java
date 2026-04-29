package net.diveon.backend.domain.problem.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import net.diveon.backend.domain.user.entity.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "comments")
public class ProblemComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id") // PK
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) // ManyToOne - Many(댓글) -> One(문제) 문제 하나에 댓글 여러개 가능
    @JoinColumn(name = "prob_id", nullable = false) // FK
    private Problem problem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false) // FK
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id") // 자기 자신 테이블 참조 FK
    private ProblemComment parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.REMOVE, orphanRemoval = true) // 원댓글 삭제 시 답글 전체 삭제
    private List<ProblemComment> replies = new ArrayList<>();

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "is_private", nullable = false)
    private Boolean isPrivate;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime updatedAt;

    public ProblemComment() {
    }

    public ProblemComment(Problem problem, User author, ProblemComment parent, String content, Boolean isPrivate) {
        this.problem = problem;
        this.author = author;
        this.parent = parent;
        this.content = content;
        this.isPrivate = isPrivate != null ? isPrivate : false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public Long getId() { return id; }
    public Problem getProblem() { return problem; }
    public User getAuthor() { return author; }
    public ProblemComment getParent() { return parent; }
    public String getContent() { return content; }
    public Boolean getIsPrivate() { return isPrivate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void updateComment(String content, Boolean isPrivate) {
        if (content != null) this.content = content;
        if (isPrivate != null) this.isPrivate = isPrivate;
        this.updatedAt = LocalDateTime.now();
    }
}

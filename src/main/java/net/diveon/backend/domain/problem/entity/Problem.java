package net.diveon.backend.domain.problem.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.GenerationType;
import jakarta.persistence.FetchType;

import net.diveon.backend.domain.problem.entity.ProblemComment;
import net.diveon.backend.domain.user.entity.User;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;


@Entity
@Table(name = "problem_summary")
public class Problem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // 출제자 정보를 담는 외래키 (유저 엔티티의 ID 타입이 String이므로 매핑 시 주의)
    // 참고로 referencedColumnName 속성 지정안해주면, 해당 엔티티(테이블)의 pk로자동매핑
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @OneToMany(mappedBy = "problem", cascade = CascadeType.REMOVE, orphanRemoval = true) // 문제 삭제 시 댓글 전체 삭제
    private List<ProblemComment> comments = new ArrayList<>();

    @Column(name = "type", length = 20, nullable = false)
    private String type; // objective / coding / practice

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Column(name = "category", length = 50, nullable = false)
    private String category;

    @Column(name = "difficulty", length = 10, nullable = false)
    private String difficulty; // easy / medium / hard

    //변수명 오타, 수정해야함.
    @Column(name = "visibility", length = 10, nullable = false)
    private String visibility; // public / private / group 

    @Column(name = "solved_count", nullable = false)
    private Integer solvedCount = 0; // 객체 생성시에 바로 0으로 되도록, 가능하면 생성자 통해서 하세용

    @Column(name = "submitted_count", nullable = false)
    private Integer submittedCount = 0; //// 객체 생성시에 바로 0으로 되도록, 가능하면 생성자 통해서 하세용

    // @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime updatedAt;

    // 1. JPA를 위한 기본 생성자 (필수)
    public Problem() {
    }

    // 2. 문제 생성을 위한 생성자
    public Problem(User author, String type, String title, String category, 
                   String difficulty, String visibility) {
        this.author = author;
        this.type = type;
        this.title = title;
        this.category = category;
        this.difficulty = difficulty;
        this.visibility = visibility;
        this.solvedCount = 0; // 초기값 0
        this.submittedCount = 0; // 초기값 0
        this.createdAt = LocalDateTime.now(); // 생성 시간 기록
        this.updatedAt = this.createdAt;
    }

    // 3. Getter 메서드들 (롬복 @Getter 대체)
    public Long getId() { return id; }
    public User getAuthor() { return author; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getCategory() { return category; }
    public String getDifficulty() { return difficulty; }
    public String getVisibility() { return visibility; }
    public Integer getSolvedCount() { return solvedCount; }
    public Integer getSubmittedCount() { return submittedCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpatedAt() { return updatedAt; }
    // 4. 비즈니스 로직 (Setter 대신 의미 있는 메서드 사용)
    public void incrementSubmittedCount() {
        this.submittedCount++;
    }

    public void incrementSolvedCount() {
        this.solvedCount++;
    }
    
    /**
     * <pre>
     * 수정-추가- 안상완
     * 2024-04-21
     * 
     * 추가 목적
     * update용 함수 
     * 
     * 변경 가능한 요소
     * title, difficulty, visibillity
     * 
     * 내용
     * null 이 아닌 값에 대해서만 변경을 진행함.
     * </pre>
     */
    public void updateProblem(String title, String category, String difficulty, String visibillity){
        if( title != null) this.title = title;
        if( category != null) this.category = category;
        if( difficulty != null) this.difficulty = difficulty;
        if( visibillity != null) this.visibility = visibillity;
        this.updatedAt = LocalDateTime.now();
    }
}

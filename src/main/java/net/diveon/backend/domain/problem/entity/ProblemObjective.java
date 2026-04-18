package net.diveon.backend.domain.problem.entity;


// 이거는 세부적으로 나누는게 좋을 듯합니다, 일단 구현에 먼저 의미를 두어서 그냥 했습니다. - 안상완
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import net.diveon.backend.domain.problem.others.Choice;
import net.diveon.backend.domain.problem.others.OboStep;
import org.hibernate.annotations.Type;

import java.util.List;

@Entity
@Table(name = "problem_objective")
public class ProblemObjective {

    @Id
    @Column(name = "prob_id")
    private Long probId;

    // 1:1 관계 설정: 부모인 Problem의 ID를 그대로 자신의 PK로 사용 (MapsId)

    // JPA에서는 모든 @entity에는 반드시 @Id가 적용된 컬럼이 하나 있어야함.
    // 그래서 @MapsId라는 어노테이션으로, 이 아래의 변수 값을 위의 @Id에 매핑시기키는 것(FK + unique 라서)
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "prob_id")
    private Problem problem;

    // PostgreSQL에서 .md를 처리해야하므로, 가장 긴 형테의 데이터 담을 수 있는 TEXT 타입
    @Column(name = "summary", columnDefinition = "TEXT", nullable = false)
    private String summary;

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;

    // Hypersistence Utils를 사용해서 List<Choice>를 PostgreSQL JSONB에 그대로 매핑합니다.
    @Type(JsonType.class)
    @Column(name = "choices", columnDefinition = "JSONB", nullable = false)
    private List<Choice> choices;

    @Type(JsonType.class)
    @Column(name = "answer", columnDefinition = "JSON", nullable = false)
    private List<Integer> answer;

    @Column(name = "obo_enabled", nullable = false)
    private Boolean oboEnabled = false;

    @Type(JsonType.class)
    @Column(name = "obo_steps", columnDefinition = "JSONB")
    private List<OboStep> oboSteps;

    // 1. JPA용 기본 생성자
    public ProblemObjective() {
    }

    // 2. 데이터 생성을 위한 생성자
    public ProblemObjective(Problem problem, String summary, String description, 
                            List<Choice> choices, List<Integer> answer,
                            Boolean oboEnabled, List<OboStep> oboSteps) {
        this.problem = problem;
        this.summary = summary;
        this.description = description;
        this.choices = choices;
        this.answer = answer;
        this.oboEnabled = (oboEnabled != null) ? oboEnabled : false;
        this.oboSteps = oboSteps;
    }

    // 3. Getter 메서드 (데이터 조회용)
    public Long getProbId() { return probId; }
    public Problem getProblem() { return problem; }
    public String getSummary() { return summary; }
    public String getDescription() { return description; }
    public List<Choice> getChoices() { return choices; }
    public List<Integer> getAnswer() { return answer; }
    public Boolean getOboEnabled() { return oboEnabled; }
    public List<OboStep> getOboSteps() { return oboSteps; }
}

package net.diveon.backend.domain.grade.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
public class SolveResultCoding {

    @Id
    @Column(name = "result_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "result_id", nullable = false)
    // 현재는 DB레벨에서 처리하지만 다음과같이 JPA에서 처리하도록 수정이 가능함. 차후 비교를 통해 변경할 것이라면 논의가 필요하다.
    @OnDelete(action = OnDeleteAction.CASCADE)
    private SolveResult result;

    @Column(name = "score")
    private Short score = 0;

    @Column(name = "memory_usage")
    private Integer memoryUsage = 0;

    @Column(name = "runtime")
    private Integer runtime = 0;

    @Column(name = "code_size")
    private Integer codeSize = 0;

    public SolveResultCoding() {}

    public SolveResultCoding(
        SolveResult result,
        Short score,
        Integer memoryUsage,
        Integer runtime,
        Integer codeSize
    ) {
        this.result = result;
        this.score = score;
        this.memoryUsage = memoryUsage;
        this.runtime = runtime;
        this.codeSize = codeSize;
    }

    public Long getId() {
        return id;
    }

    public SolveResult getResult() {
        return result;
    }

    public Short getScore() {
        return score;
    }

    public Integer getMemoryUsage() {
        return memoryUsage;
    }

    public Integer getRuntime() {
        return runtime;
    }

    public Integer getCodeSize() {
        return codeSize;
    }
}

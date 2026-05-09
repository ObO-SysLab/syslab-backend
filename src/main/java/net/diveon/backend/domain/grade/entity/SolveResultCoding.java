package net.diveon.backend.domain.grade.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;

@Entity
public class SolveResultCoding {

    @Id
    @Column(name = "result_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "result_id", nullable = false)
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

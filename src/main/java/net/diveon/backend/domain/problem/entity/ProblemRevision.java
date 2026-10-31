package net.diveon.backend.domain.problem.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity @Immutable
@Table(name="problem_revision", indexes=@Index(name="ix_problem_revision_problem",columnList="problem_id"))
public class ProblemRevision {
    @Id @Column(length=96,updatable=false) private String id;
    @Column(name="problem_id",nullable=false,updatable=false) private Long problemId;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition="jsonb",nullable=false,updatable=false) private JsonNode snapshot;
    @Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt;
    protected ProblemRevision() {}
    public ProblemRevision(String id,Long problemId,JsonNode snapshot){
        this.id=id;this.problemId=problemId;this.snapshot=snapshot.deepCopy();this.createdAt=LocalDateTime.now();
    }
    public String getId(){return id;}
    public Long getProblemId(){return problemId;}
    public JsonNode getSnapshot(){return snapshot.deepCopy();}
}

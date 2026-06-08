package net.diveon.backend.domain.grade.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import net.diveon.backend.domain.grade.entity.SolveResult;

public interface SolveResultRepository extends JpaRepository<SolveResult, Long>{

    Optional<SolveResult> findBySubmissionId(Long submissionId);

    @Query("""
        SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
        FROM SolveResult r
        WHERE r.submission.user.id = :userId
          AND r.submission.problem.id = :probId
          AND r.resultState = 'CORRECT'
        """)
    boolean existsCorrectResultByUserIdAndProblemId(
        @Param("userId") Long userId,
        @Param("probId") Long probId
    );
}

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

    @Query(value = """
        SELECT COALESCE(SUM(solved.score), 0)
        FROM (
            SELECT
                CASE ps.difficulty
                    WHEN '1' THEN 10
                    WHEN '2' THEN 20
                    WHEN '3' THEN 30
                    WHEN '4' THEN 40
                    WHEN '5' THEN 50
                    WHEN '6' THEN 60
                    WHEN '7' THEN 70
                    ELSE 0
                END AS score
            FROM solve_result sr
            JOIN solve_submission ss ON sr.submission_id = ss.id
            JOIN problem_summary ps ON ss.prob_id = ps.id
            WHERE ss.submitter_id = :userId
              AND sr.result_status = 'CORRECT'
              AND ps.visibility NOT IN ('group', 'contest')
            GROUP BY ps.id, ps.difficulty
            ORDER BY score DESC
            LIMIT 100
        ) solved
        """, nativeQuery = true)
    Integer calculateTierScoreByUserId(@Param("userId") Long userId);
}

package net.diveon.backend.domain.problem.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import net.diveon.backend.domain.problem.entity.Problem;

import java.util.List;
import java.util.Optional;

public interface ProblemRepository extends JpaRepository<Problem, Long>, JpaSpecificationExecutor<Problem> {

    @Query(value = """
            SELECT * FROM problem_summary p
            WHERE p.visibility = 'public'
            AND (p.type != 'practice' OR EXISTS (
                SELECT 1 FROM problem_practice pp WHERE pp.prob_id = p.id AND pp.image_status = 'READY'
            ))
            ORDER BY RANDOM()
            LIMIT 1
            """, nativeQuery = true)
    Optional<Problem> findRandomPublicProblem();

    @Query(value = """
            SELECT * FROM problem_summary p
            WHERE p.visibility = 'public'
              AND p.category = :category
              AND p.id NOT IN (
                  SELECT ss.prob_id FROM solve_submission ss WHERE ss.submitter_id = :userId
              )
              AND (p.type != 'practice' OR EXISTS (
                  SELECT 1 FROM problem_practice pp WHERE pp.prob_id = p.id AND pp.image_status = 'READY'
              ))
            ORDER BY p.solved_count DESC
            LIMIT 3
            """, nativeQuery = true)
    List<Problem> findRecommendByCategory(@Param("userId") Long userId,
                                          @Param("category") String category);

    @Query(value = """
            SELECT * FROM problem_summary p
            WHERE p.visibility = 'public'
              AND p.id NOT IN (
                  SELECT ss.prob_id FROM solve_submission ss WHERE ss.submitter_id = :userId
              )
              AND (p.type != 'practice' OR EXISTS (
                  SELECT 1 FROM problem_practice pp WHERE pp.prob_id = p.id AND pp.image_status = 'READY'
              ))
            ORDER BY p.solved_count DESC
            LIMIT 3
            """, nativeQuery = true)
    List<Problem> findPopularUnsolvedProblems(@Param("userId") Long userId);

    @Query(value = """
            SELECT DISTINCT p.* FROM problem_summary p
            JOIN solve_submission ss ON ss.prob_id = p.id
            JOIN solve_result sr ON sr.submission_id = ss.id
            WHERE ss.submitter_id = :userId
              AND sr.result_status = 'CORRECT'
              AND (:visibility IS NULL OR p.visibility = :visibility)
            ORDER BY p.id DESC
            """,
            countQuery = """
            SELECT COUNT(DISTINCT p.id) FROM problem_summary p
            JOIN solve_submission ss ON ss.prob_id = p.id
            JOIN solve_result sr ON sr.submission_id = ss.id
            WHERE ss.submitter_id = :userId
              AND sr.result_status = 'CORRECT'
              AND (:visibility IS NULL OR p.visibility = :visibility)
            """,
            nativeQuery = true)
    Page<Problem> findSolvedByUserId(@Param("userId") Long userId,
                                     @Param("visibility") String visibility,
                                     Pageable pageable);

    @Query(value = """
            SELECT DISTINCT p.* FROM problem_summary p
            JOIN solve_submission ss ON ss.prob_id = p.id
            JOIN solve_result sr ON sr.submission_id = ss.id
            WHERE ss.submitter_id = :userId
              AND sr.result_status = 'WRONG'
              AND (:visibility IS NULL OR p.visibility = :visibility)
              AND NOT EXISTS (
                  SELECT 1 FROM solve_submission ss2
                  JOIN solve_result sr2 ON sr2.submission_id = ss2.id
                  WHERE ss2.submitter_id = :userId
                    AND ss2.prob_id = p.id
                    AND sr2.result_status = 'CORRECT'
              )
            ORDER BY p.id DESC
            """,
            countQuery = """
            SELECT COUNT(DISTINCT p.id) FROM problem_summary p
            JOIN solve_submission ss ON ss.prob_id = p.id
            JOIN solve_result sr ON sr.submission_id = ss.id
            WHERE ss.submitter_id = :userId
              AND sr.result_status = 'WRONG'
              AND (:visibility IS NULL OR p.visibility = :visibility)
              AND NOT EXISTS (
                  SELECT 1 FROM solve_submission ss2
                  JOIN solve_result sr2 ON sr2.submission_id = ss2.id
                  WHERE ss2.submitter_id = :userId
                    AND ss2.prob_id = p.id
                    AND sr2.result_status = 'CORRECT'
              )
            """,
            nativeQuery = true)
    Page<Problem> findFailedByUserId(@Param("userId") Long userId,
                                     @Param("visibility") String visibility,
                                     Pageable pageable);

    Page<Problem> findByAuthorIdAndVisibility(Long authorId, String visibility, Pageable pageable);

    Page<Problem> findByAuthorId(Long authorId, Pageable pageable);
}

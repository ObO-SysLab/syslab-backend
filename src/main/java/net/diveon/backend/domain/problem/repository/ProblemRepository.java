package net.diveon.backend.domain.problem.repository;

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
}

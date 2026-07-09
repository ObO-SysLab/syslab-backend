package net.diveon.backend.domain.problem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import net.diveon.backend.domain.problem.entity.Problem;

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
}

package net.diveon.backend.domain.grade.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import net.diveon.backend.domain.grade.entity.SolveSubmission;

public interface SolveSubmissionRepository extends JpaRepository<SolveSubmission, Long>{

    @Query("SELECT s FROM SolveSubmission s WHERE s.problem.id = :probId AND s.submissionState = 'COMPLETED'")
    Page<SolveSubmission> findCompletedByProbId(@Param("probId") Long probId, Pageable pageable);

    @Query("SELECT COUNT(s) FROM SolveSubmission s WHERE s.problem.id = :probId AND s.submissionState = 'COMPLETED'")
    Long countCompletedByProbId(@Param("probId") Long probId);

    @Query("SELECT r.submission.problem.id FROM SolveResult r WHERE r.submission.user.id = :userId AND r.resultState = 'CORRECT'")
    List<Long> findSolvedProblemIdsByUserId(@Param("userId") Long userId);
}

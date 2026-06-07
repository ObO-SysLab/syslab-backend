package net.diveon.backend.domain.contest.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import net.diveon.backend.domain.contest.entity.ContestSubmission;

public interface ContestSubmissionRepository extends JpaRepository<ContestSubmission, Long> {

    @Query("""
            SELECT DISTINCT cs.contestProblem.id
            FROM ContestSubmission cs
            WHERE cs.contest.id = :contestId
              AND cs.user.id = :userId
              AND cs.isCorrect = true
            """)
    List<Long> findSolvedContestProblemIds(@Param("contestId") Long contestId, @Param("userId") Long userId);

    @Query("""
            SELECT cs.user.id, COUNT(DISTINCT cs.contestProblem.id)
            FROM ContestSubmission cs
            WHERE cs.contest.id = :contestId AND cs.isCorrect = true
            GROUP BY cs.user.id
            """)
    List<Object[]> countSolvedByUserForContest(@Param("contestId") Long contestId);

    Optional<ContestSubmission> findBySolveSubmissionId(Long solveSubmissionId);

    @Query("""
            SELECT cs.contestProblem.id, COUNT(cs)
            FROM ContestSubmission cs
            WHERE cs.contest.id = :contestId
            GROUP BY cs.contestProblem.id
            """)
    List<Object[]> countSubmissionsPerProblem(@Param("contestId") Long contestId);

    @Query("""
            SELECT cs.contestProblem.id, COUNT(cs)
            FROM ContestSubmission cs
            WHERE cs.contest.id = :contestId AND cs.isCorrect = true
            GROUP BY cs.contestProblem.id
            """)
    List<Object[]> countSolvedPerProblem(@Param("contestId") Long contestId);
}

package net.diveon.backend.domain.contest.repository;

import java.util.List;

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
}

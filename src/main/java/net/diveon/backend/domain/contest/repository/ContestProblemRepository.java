package net.diveon.backend.domain.contest.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import net.diveon.backend.domain.contest.entity.ContestProblem;

public interface ContestProblemRepository extends JpaRepository<ContestProblem, Long> {
    List<ContestProblem> findAllByContestId(Long contestId);

    @Query("SELECT cp FROM ContestProblem cp JOIN FETCH cp.problem WHERE cp.contest.id = :contestId ORDER BY cp.id ASC")
    List<ContestProblem> findAllWithProblemByContestId(@Param("contestId") Long contestId);
}

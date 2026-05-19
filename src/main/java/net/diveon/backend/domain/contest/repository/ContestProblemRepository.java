package net.diveon.backend.domain.contest.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import net.diveon.backend.domain.contest.entity.ContestProblem;

public interface ContestProblemRepository extends JpaRepository<ContestProblem, Long> {
    List<ContestProblem> findAllByContestId(Long contestId);
}

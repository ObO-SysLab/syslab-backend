package net.diveon.backend.domain.problem.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import net.diveon.backend.domain.problem.entity.ProblemObjective;

public interface ProblemRepository extends JpaRepository<ProblemObjective, Long> {
    
}

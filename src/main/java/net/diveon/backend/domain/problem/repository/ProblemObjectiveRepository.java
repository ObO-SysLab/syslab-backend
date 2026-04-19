package net.diveon.backend.domain.problem.repository;

import net.diveon.backend.domain.problem.entity.ProblemObjective;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProblemObjectiveRepository extends JpaRepository<ProblemObjective, Long>{
    
}

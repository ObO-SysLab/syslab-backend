package net.diveon.backend.domain.problem.repository;

import net.diveon.backend.domain.problem.entity.Problem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProblemObjectiveRepository extends JpaRepository<Problem, Long>{
    
}

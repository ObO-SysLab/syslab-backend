package net.diveon.backend.domain.problem.repository;

import net.diveon.backend.domain.problem.entity.OboStep;
import net.diveon.backend.domain.problem.entity.Problem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OboStepRepository extends JpaRepository<OboStep, Long> {
    List<OboStep> findByProblemOrderByStepAsc(Problem problem);
}

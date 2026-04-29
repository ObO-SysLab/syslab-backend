package net.diveon.backend.domain.problem.repository;

import net.diveon.backend.domain.problem.entity.OboStep;
import net.diveon.backend.domain.problem.entity.Problem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OboStepRepository extends JpaRepository<OboStep, Long> {
    /**
     * 이놈과  List<OboStep> findByProblemOrderByStepAsc(Problem problem);  
     * 이놈은List<OboStep> findByProblem_IdOrderByStepAsc(Long probId);
     * 조건에 따라서 다르게 쓸 수 있음, 만약 prob_id가 안주어지면 처음이 효율적임
     * 
     */

    List<OboStep> findByProblemOrderByStepAsc(Problem problem); 
    Optional<OboStep> findByProblem_IdAndStep(Long probId, Integer step);
    List<OboStep> findByProblem_IdOrderByStepAsc(Long probId);
    void deleteByProblem_Id(Long probId);
}

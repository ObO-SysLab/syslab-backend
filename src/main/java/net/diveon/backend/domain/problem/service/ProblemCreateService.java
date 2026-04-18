package net.diveon.backend.domain.problem.service;

import org.springframework.stereotype.Controller;

import net.diveon.backend.domain.problem.dto.request.ProblemCreateObjectiveRequest;
import net.diveon.backend.domain.problem.entity.Problem;
import net.diveon.backend.domain.problem.repository.ProblemObjectiveRepository;
import net.diveon.backend.domain.problem.repository.ProblemRepository;

@Controller
public class ProblemCreateService {
    private final ProblemObjectiveRepository problemObjectiveRepository;
    private final ProblemRepository problemRepository;


    public ProblemCreateService(ProblemObjectiveRepository problemObjectiveRepository 
        ,ProblemRepository problemRepository){
        this.problemObjectiveRepository = problemObjectiveRepository;
        this.problemRepository = problemRepository;
    }


    public void createObjective(ProblemCreateObjectiveRequest request){
        // 먼저 problem table에 해당하는 entity 생성하고 저장
        // 이후 problem_objective에 해당하는 entity 생성하고 저장

        Problem problem = new Problem();
        
    }
}

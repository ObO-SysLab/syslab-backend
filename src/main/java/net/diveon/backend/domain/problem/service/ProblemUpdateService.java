package net.diveon.backend.domain.problem.service;

import org.springframework.transaction.annotation.Transactional;

import net.diveon.backend.domain.problem.dto.request.ProblemUpdateObjectiveRequest;
import net.diveon.backend.domain.problem.dto.response.ProblemCreateObjectiveResponse;
import net.diveon.backend.domain.problem.dto.response.ProblemUpdateObjectiveResponse;
import net.diveon.backend.domain.problem.entity.Problem;
import net.diveon.backend.domain.problem.entity.ProblemObjective;
import net.diveon.backend.domain.problem.repository.ProblemObjectiveRepository;
import net.diveon.backend.domain.problem.repository.ProblemRepository;
import net.diveon.backend.domain.user.repository.UserRepository;

public class ProblemUpdateService {
    private final ProblemRepository problemRepository;
    private final ProblemObjectiveRepository problemObjectiveRepository;
    private final UserRepository userRepository;

    public ProblemUpdateService(ProblemRepository problemRepository,
        ProblemObjectiveRepository problemObjectiveRepository,
        UserRepository userRepository){
            this.problemObjectiveRepository = problemObjectiveRepository;
            this.problemRepository = problemRepository;
            this.userRepository = userRepository;
        }

    
    @Transactional
    public ProblemUpdateObjectiveResponse updateProblemObjective(String userId, long prodId, 
        ProblemUpdateObjectiveRequest request){
            Problem problem = problemRepository.findById(prodId).orElseThrow();

            ProblemObjective problemObjective = problemObjectiveRepository.findById(prodId).orElseThrow();

            problem.updateProblem(request.getTitle(), request.getDifficulty(), request.getVisibility());
            problemObjective.updateProblemObjective(request.getTitle(), request.getDescription(), request.getChoices(), request.getAnswer(), request.geto);


            return new ProblemUpdateObjectiveResponse(prodId, "type", "title", "updatedat");
    }

    
}

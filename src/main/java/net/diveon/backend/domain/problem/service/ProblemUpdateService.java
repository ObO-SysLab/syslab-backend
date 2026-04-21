package net.diveon.backend.domain.problem.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.diveon.backend.domain.problem.dto.request.ProblemUpdateObjectiveRequest;
import net.diveon.backend.domain.problem.dto.response.ProblemCreateObjectiveResponse;
import net.diveon.backend.domain.problem.dto.response.ProblemUpdateObjectiveResponse;
import net.diveon.backend.domain.problem.entity.Problem;
import net.diveon.backend.domain.problem.entity.ProblemObjective;
import net.diveon.backend.domain.problem.repository.ProblemObjectiveRepository;
import net.diveon.backend.domain.problem.repository.ProblemRepository;
import net.diveon.backend.domain.user.repository.UserRepository;


@Service
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
    public ProblemUpdateObjectiveResponse updateProblemObjective(long userId, long prodId, 
        ProblemUpdateObjectiveRequest request){
            Problem problem = problemRepository.findById(prodId).orElseThrow();

            ProblemObjective problemObjective = problemObjectiveRepository.findById(prodId).orElseThrow();

            problem.updateProblem(request.getTitle(), request.getDifficulty(), request.getVisibility());

            // 여기서 request.getObo().getEnabled() 으로 넣는데, 이때 null 이면 null.getEnabled() 라고되어서 문제 생김 나중에 수정이 필요함.
            problemObjective.updateProblemObjective(request.getTitle(), request.getDescription(), 
            request.getChoices(), request.getAnswer(), 
            null, null);


            return new ProblemUpdateObjectiveResponse(prodId, request.getCategory(), request.getTitle(), problem.getUpatedAt().toString());
    }

    
}

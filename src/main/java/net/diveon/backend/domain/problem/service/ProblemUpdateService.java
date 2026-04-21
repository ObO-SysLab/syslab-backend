package net.diveon.backend.domain.problem.service;

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

    
    public ProblemUpdateObjectiveResponse updateProblemObjective(String userId, long prodId, 
        ProblemUpdateObjectiveRequest request){
            Problem problem = problemRepository.findById(prodId).orElseThrow();

            ProblemObjective problemObjective = problemObjectiveRepository.findById(prodId).orElseThrow();

            //Problem entitiy 수정
            if(request.getTitle() != null){

            }
            if(request.getCategory() != null){

            }
            if(request.getDifficulty() != null){

            }
            if(request.getVisibility() != null){

            }

            // ProblemObjective Entity 수정
            if(request.getTitle() != null){

            }
            if(request.getDescription() != null){

            }
            if (request.getChoices() != null) {
                
            }
            if (request.getAnswer() != null) {
                
            }            
            Boolean oboEnabled = request.getObo() != null ? request.getObo().getEnabled() : false; 
               
            if((request.getObo() != null ? request.getObo().getSteps() : null)  != null){

            }

                // oboEnabled,
                // request.getObo() != null ? request.getObo().getSteps() : null

        //             User author = userRepository.findById(userId)
        //         .orElseThrow();

        // Problem problem = new Problem(
        //         author,
        //         "objective",
        //         request.getTitle(),
        //         request.getCategory(),
        //         request.getDifficulty(),
        //         request.getVisibility()
        // );
        // Problem savedProblem = problemRepository.save(problem);

        // Boolean oboEnabled = request.getObo() != null ? request.getObo().getEnabled() : false;
        // ProblemObjective problemObjective = new ProblemObjective(
        //         savedProblem,
        //         request.getTitle(), // 요놈은 summary인데 없어서 일단 가정으로 추가함.
        //         request.getDescription(),
        //         request.getChoices(),
        //         request.getAnswer(),
        //         oboEnabled,
        //         request.getObo() != null ? request.getObo().getSteps() : null
        // );
        // problemObjectiveRepository.save(problemObjective);

        // return new ProblemCreateObjectiveResponse(
        //         savedProblem.getId(),
        //         savedProblem.getType(),
        //         savedProblem.getTitle(), 
        //         savedProblem.getCreatedAt().toString()
        // );


            return new ProblemUpdateObjectiveResponse(prodId, "type", "title", "updatedat");
    }

    
}

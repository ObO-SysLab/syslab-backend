package net.diveon.backend.domain.problem.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.diveon.backend.domain.problem.dto.request.ProblemUpdateObjectiveRequest;
import net.diveon.backend.domain.problem.entity.OboStep;
import net.diveon.backend.domain.problem.dto.response.ProblemUpdateObjectiveResponse;
import net.diveon.backend.domain.problem.entity.Problem;
import net.diveon.backend.domain.problem.entity.ProblemObjective;
import net.diveon.backend.domain.problem.others.ForDtoOboStep;
import net.diveon.backend.domain.problem.repository.OboStepRepository;
import net.diveon.backend.domain.problem.repository.ProblemObjectiveRepository;
import net.diveon.backend.domain.problem.repository.ProblemRepository;
import net.diveon.backend.domain.user.repository.UserRepository;

import java.util.List;


@Service
public class ProblemUpdateService {
    private final ProblemRepository problemRepository;
    private final ProblemObjectiveRepository problemObjectiveRepository;
    private final OboStepRepository oboStepRepository;
    private final UserRepository userRepository;

    public ProblemUpdateService(ProblemRepository problemRepository,
        ProblemObjectiveRepository problemObjectiveRepository,
        OboStepRepository oboStepRepository,
        UserRepository userRepository){
            this.problemObjectiveRepository = problemObjectiveRepository;
            this.problemRepository = problemRepository;
            this.oboStepRepository = oboStepRepository;
            this.userRepository = userRepository;
        }

    
    @Transactional
    public ProblemUpdateObjectiveResponse updateProblemObjective(long userId, long prodId, 
        ProblemUpdateObjectiveRequest request){
            Problem problem = problemRepository.findById(prodId).orElseThrow();

            ProblemObjective problemObjective = problemObjectiveRepository.findById(prodId).orElseThrow();

            problem.updateProblem(request.getTitle(), request.getDifficulty(), request.getVisibility());

            problemObjective.updateProblemObjective(
                request.getSummary(),
                request.getDescription(),
                request.getChoices(),
                request.getAnswer(),
                request.getOboEnabled()
            );

            // List<OboStep> existingSteps = oboStepRepository.findByProblemOrderByStepAsc(problem);
            // if (!existingSteps.isEmpty()) {
            //     oboStepRepository.deleteAll(existingSteps);
            // }

            //이 아래의 로직은, obo step에 대해서, 일단 전부 삭제하고 전부 삽입하는것이라, 
            // update에서 아무 값도 안주면 바로 다 삭제되는 방식입니다.


            /**
             * TODO : 현재 객관식 업데이트에서 obo step을 업데이트 하는 방법을 고도화 하기
             * 고도화 하는 방법은 다음과 같음
             * 1. 현재 삭제 방식을 다듬어서, 값이 주어지지 않는다면, 삭제가 되지 않도록 하기
             * 2. 개별 삭제 기능을 구현할 것.
             */
            oboStepRepository.deleteByProblem_Id(prodId);

            // 개별 업데이트 개선 필요
            if (request.getObo() != null && request.getObo().getSteps() != null && !request.getObo().getSteps().isEmpty()) {
                List<OboStep> oboSteps = request.getObo().getSteps().stream()
                    .map(step -> toOboStep(problem, step))
                    .toList();
                oboStepRepository.saveAll(oboSteps);
            }

            return new ProblemUpdateObjectiveResponse(prodId, request.getCategory(), request.getTitle(), problem.getUpatedAt().toString());
    }

    private OboStep toOboStep(Problem problem, ForDtoOboStep step) {
        return new OboStep(problem, step.getStep(), step.getDescription(), step.getImageUrl());
    }
}

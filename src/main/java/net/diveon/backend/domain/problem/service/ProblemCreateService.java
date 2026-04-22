package net.diveon.backend.domain.problem.service;

import net.diveon.backend.domain.problem.dto.request.ProblemCreateObjectiveRequest;
import net.diveon.backend.domain.problem.dto.request.ProblemCreateCodingRequest;
import net.diveon.backend.domain.problem.dto.response.ProblemCreateObjectiveResponse;
import net.diveon.backend.domain.problem.entity.OboStep;
import net.diveon.backend.domain.problem.entity.Problem;
import net.diveon.backend.domain.problem.entity.ProblemObjective;
import net.diveon.backend.domain.problem.others.ForDtoOboStep;
import net.diveon.backend.domain.problem.repository.OboStepRepository;
import net.diveon.backend.domain.problem.dto.response.ProblemCreateCodingResponse;
import net.diveon.backend.domain.problem.entity.ProblemCoding;
import net.diveon.backend.domain.problem.repository.ProblemObjectiveRepository;
import net.diveon.backend.domain.problem.repository.ProblemCodingRepository;
import net.diveon.backend.domain.problem.repository.ProblemRepository;
import net.diveon.backend.domain.user.entity.User;
import net.diveon.backend.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProblemCreateService {
    private final ProblemObjectiveRepository problemObjectiveRepository;
    private final OboStepRepository oboStepRepository;
    private final ProblemCodingRepository problemCodingRepository;
    private final ProblemRepository problemRepository;
    private final UserRepository userRepository;

    public ProblemCreateService(ProblemObjectiveRepository problemObjectiveRepository,
                              ProblemCodingRepository problemCodingRepository,
                              ProblemRepository problemRepository,
                              UserRepository userRepository,
                        OboStepRepository oboStepRepository){
        this.problemObjectiveRepository = problemObjectiveRepository;
        this.problemCodingRepository = problemCodingRepository;
        this.problemRepository = problemRepository;
        this.userRepository = userRepository;
        this.oboStepRepository = oboStepRepository;
    }


    @Transactional
    public ProblemCreateObjectiveResponse createObjective(ProblemCreateObjectiveRequest request, String userId){
        User author = userRepository.findById(Long.parseLong(userId))
                .orElseThrow();

        Problem problem = new Problem(
                author,
                "objective",
                request.getTitle(),
                request.getCategory(),
                request.getDifficulty(),
                request.getVisibility()
        );
        Problem savedProblem = problemRepository.save(problem);

        Boolean oboEnabled = request.getOboEnabled() != null ? request.getOboEnabled() : false;
        ProblemObjective problemObjective = new ProblemObjective(
                savedProblem,
                request.getSummary(),
                request.getDescription(),
                request.getChoices(),
                request.getAnswer(),
                oboEnabled
        );
        problemObjectiveRepository.save(problemObjective);
        //TODO : 이 로직이 왜 이런건지 확인하기
        // 아 그리고 지금 구조가 request.getObo().getSteps() 이런식이라 
        // 이건 request를 수정하든 다른 방식을추가해야할듯. 
        // 뻑나기 쉬워보임null.getSteps() 될거같음
        if (request.getObo() != null && request.getObo().getSteps() != null && !request.getObo().getSteps().isEmpty()) {
            List<OboStep> oboSteps = request.getObo().getSteps().stream()
                .map(step -> toOboStep(savedProblem, step))
                .toList();
            oboStepRepository.saveAll(oboSteps);
        }

        /**
         * 위 함수 설명
         * .stream() // 이 리스트를 스트림으로 바꿉니다. 즉 “각 원소를 순서대로 가공하겠다”는 뜻입니
         * 
         * .map(step -> toOboStep(savedProblem, step))
         * 각 ForDtoOboStep 원소를 OboStep으로 변환합니다.
         * 여기서 step -> ...은 람다식이고,
         * 의미는 “리스트의 각 step에 대해 이 함수를 적용해라”입니다.
         * 
         * .toList()
         * 변환된 결과들을 다시 List로 모읍니다.
         */

        return new ProblemCreateObjectiveResponse(
                savedProblem.getId(),
                savedProblem.getType(),
                savedProblem.getTitle(),
                savedProblem.getCreatedAt().toString()
        );
    }

    @Transactional
    public ProblemCreateCodingResponse createCoding(ProblemCreateCodingRequest request, String userId) {
        User author = userRepository.findById(Long.parseLong(userId))
                .orElseThrow();

        Problem problem = new Problem(
                author,
                "coding",
                request.getTitle(),
                request.getCategory(),
                request.getDifficulty(),
                request.getVisibility()
        );
        Problem savedProblem = problemRepository.save(problem);

        Boolean oboEnabled = request.getObo() != null ? request.getObo().getEnabled() : false;
        String oboInitialImageUrl = request.getObo() != null ? request.getObo().getInitialImageUrl() : null;

        ProblemCoding problemCoding = new ProblemCoding(
                savedProblem,
                request.getSummary(),
                request.getDescription(),
                request.getInputDescription(),
                request.getOutputDescription(),
                request.getConstraints().getTimeLimitMs(),
                request.getConstraints().getMemoryLimitMb(),
                request.getConstraints().getAllowedLanguages(),
                request.getTestcases(),
                request.getFileUrl(),
                oboEnabled,
                oboInitialImageUrl
        );
        problemCodingRepository.save(problemCoding);

        return new ProblemCreateCodingResponse(
                savedProblem.getId(),
                savedProblem.getType(),
                savedProblem.getTitle(),
                savedProblem.getCreatedAt().toString()
        );
    }
    /**
     * <pre>
     * ForDtoOboStep 내부 List 형식에서, Obosetp 라는 엔티티로 변환하기 위한 함수 
     * 
     * 왜냐?
     * 이전 JSONB에 직렬화 해서 때려박는건, JSONB한덩어리에 때려박는거라 그대로 직렬화가 됨
     * 근데 이제는 테이블이 분리됨 + obo_step이라는 테이블의 구조가 하나의 row가 하나의 OboStep.java 객체임
     * 따라서 각 객체 하나하나씩 DB에 삽입해야함
     * </pre>
     * @param problem
     * @param step
     * @return
     */
    private OboStep toOboStep(Problem problem, ForDtoOboStep step) {
        return new OboStep(problem, step.getStep(), step.getDescription(), step.getImageUrl());
    }
}

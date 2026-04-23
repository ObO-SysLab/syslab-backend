package net.diveon.backend.domain.problem.service;

import net.diveon.backend.domain.problem.dto.response.ProblemDetailObjectiveResponse;
import net.diveon.backend.domain.problem.dto.response.interfaces.ProblemDetailResponse;
import net.diveon.backend.domain.problem.entity.OboStep;
import net.diveon.backend.domain.problem.entity.Problem;
import net.diveon.backend.domain.problem.entity.ProblemObjective;
import net.diveon.backend.domain.problem.repository.OboStepRepository;
import net.diveon.backend.domain.problem.repository.ProblemObjectiveRepository;
import net.diveon.backend.domain.problem.repository.ProblemRepository;
import net.diveon.backend.domain.user.repository.UserRepository;
import net.diveon.backend.global.exception.ProblemNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProblemDetailService {

    private final UserRepository userRepository;
    private final ProblemRepository problemRepository;
    private final ProblemObjectiveRepository problemObjectiveRepository;
    private final OboStepRepository oboStepRepository;

    public ProblemDetailService(
        UserRepository userRepository,
        ProblemRepository problemRepository,
        ProblemObjectiveRepository problemObjectiveRepository,
        OboStepRepository oboStepRepository
    ) {
        this.userRepository = userRepository;
        this.problemRepository = problemRepository;
        this.problemObjectiveRepository = problemObjectiveRepository;
        this.oboStepRepository = oboStepRepository;
    }

    @Transactional(readOnly = true)
    public ProblemDetailResponse detailProblem(long userId, long probId) {
        userRepository.findById(userId).orElseThrow();

        Problem problem = problemRepository.findById(probId)
            .orElseThrow(() -> new ProblemNotFoundException(probId + "번에 해당하는 문제가 존재하지 않습니다."));

        if (problem.getType().equals("objective")) {
            return detailProblemObjective(problem, probId);
        } else {
            throw new ProblemNotFoundException(probId + "번 문제의 타입을 알 수 없습니다.");
        }
    }

    private ProblemDetailObjectiveResponse detailProblemObjective(Problem problem, long probId) {
        ProblemObjective problemObjective = problemObjectiveRepository.findById(probId).orElseThrow();
        List<OboStep> oboSteps = oboStepRepository.findByProblem_IdOrderByStepAsc(probId);

        return ProblemDetailObjectiveResponse.of(problem, problemObjective, oboSteps);
    }
}

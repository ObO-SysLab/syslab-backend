package net.diveon.backend.domain.problem.service;

import net.diveon.backend.domain.grade.repository.SolveSubmissionRepository;
import net.diveon.backend.domain.problem.dto.response.ProblemListItemResponse;
import net.diveon.backend.domain.problem.entity.Problem;
import net.diveon.backend.domain.problem.repository.ProblemRepository;
import net.diveon.backend.global.exception.ProblemNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class ProblemRandomService {

    private final ProblemRepository problemRepository;
    private final SolveSubmissionRepository solveSubmissionRepository;

    public ProblemRandomService(ProblemRepository problemRepository, SolveSubmissionRepository solveSubmissionRepository) {
        this.problemRepository = problemRepository;
        this.solveSubmissionRepository = solveSubmissionRepository;
    }

    @Transactional(readOnly = true)
    public ProblemListItemResponse getRandomProblem(long userId) {
        Problem problem = problemRepository.findRandomPublicProblem()
                .orElseThrow(ProblemNotFoundException::new);

        Set<Long> solvedIds = Set.copyOf(solveSubmissionRepository.findSolvedProblemIdsByUserId(userId));
        return ProblemListItemResponse.of(problem, solvedIds.contains(problem.getId()));
    }
}

package net.diveon.backend.domain.user.service;

import net.diveon.backend.domain.grade.repository.SolveSubmissionRepository;
import net.diveon.backend.domain.problem.dto.response.ProblemListItemResponse;
import net.diveon.backend.domain.problem.dto.response.ProblemListResponse;
import net.diveon.backend.domain.problem.entity.Problem;
import net.diveon.backend.domain.problem.repository.ProblemRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class MyPageProblemService {

    private static final int PAGE_SIZE = 20;

    private final ProblemRepository problemRepository;
    private final SolveSubmissionRepository solveSubmissionRepository;

    public MyPageProblemService(ProblemRepository problemRepository,
                                SolveSubmissionRepository solveSubmissionRepository) {
        this.problemRepository = problemRepository;
        this.solveSubmissionRepository = solveSubmissionRepository;
    }

    @Transactional(readOnly = true)
    public ProblemListResponse getSolvedProblems(Long userId, int page, String visibility) {
        Page<Problem> result = problemRepository.findSolvedByUserId(userId, visibility, PageRequest.of(page - 1, PAGE_SIZE));
        List<ProblemListItemResponse> items = result.getContent().stream()
                .map(p -> ProblemListItemResponse.of(p, true))
                .toList();
        return ProblemListResponse.of(result, items);
    }

    @Transactional(readOnly = true)
    public ProblemListResponse getFailedProblems(Long userId, int page, String visibility) {
        Page<Problem> result = problemRepository.findFailedByUserId(userId, visibility, PageRequest.of(page - 1, PAGE_SIZE));
        List<ProblemListItemResponse> items = result.getContent().stream()
                .map(p -> ProblemListItemResponse.of(p, false))
                .toList();
        return ProblemListResponse.of(result, items);
    }

    @Transactional(readOnly = true)
    public ProblemListResponse getAuthoredProblems(Long userId, int page, String visibility) {
        Set<Long> solvedIds = Set.copyOf(solveSubmissionRepository.findSolvedProblemIdsByUserId(userId));
        Page<Problem> result = visibility != null
                ? problemRepository.findByAuthorIdAndVisibility(userId, visibility, PageRequest.of(page - 1, PAGE_SIZE))
                : problemRepository.findByAuthorId(userId, PageRequest.of(page - 1, PAGE_SIZE));
        List<ProblemListItemResponse> items = result.getContent().stream()
                .map(p -> ProblemListItemResponse.of(p, solvedIds.contains(p.getId())))
                .toList();
        return ProblemListResponse.of(result, items);
    }
}

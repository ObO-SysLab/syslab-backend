package net.diveon.backend.domain.problem.service;

import net.diveon.backend.domain.grade.repository.SolveResultRepository;
import net.diveon.backend.domain.grade.repository.SolveSubmissionRepository;
import net.diveon.backend.domain.problem.dto.response.ProblemListItemResponse;
import net.diveon.backend.domain.problem.dto.response.ProblemRecommendResponse;
import net.diveon.backend.domain.problem.entity.Problem;
import net.diveon.backend.domain.problem.repository.ProblemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class ProblemRecommendService {

    private final ProblemRepository problemRepository;
    private final SolveResultRepository solveResultRepository;
    private final SolveSubmissionRepository solveSubmissionRepository;

    public ProblemRecommendService(ProblemRepository problemRepository,
                                   SolveResultRepository solveResultRepository,
                                   SolveSubmissionRepository solveSubmissionRepository) {
        this.problemRepository = problemRepository;
        this.solveResultRepository = solveResultRepository;
        this.solveSubmissionRepository = solveSubmissionRepository;
    }

    @Transactional(readOnly = true)
    public ProblemRecommendResponse recommend(Long userId) {
        List<Object[]> weakCategories = solveResultRepository.findWeakCategories(userId);
        Set<Long> solvedIds = Set.copyOf(solveSubmissionRepository.findSolvedProblemIdsByUserId(userId));

        if (weakCategories.isEmpty()) {
            return fallback(userId, solvedIds);
        }

        String topCategory = (String) weakCategories.get(0)[0];
        List<Problem> recommended = problemRepository.findRecommendByCategory(userId, topCategory);

        List<ProblemListItemResponse> result = new ArrayList<>(
                recommended.stream()
                        .map(p -> ProblemListItemResponse.of(p, solvedIds.contains(p.getId())))
                        .toList()
        );

        if (result.isEmpty()) {
            return fallback(userId, solvedIds);
        }

        if (result.size() < 3) {
            Set<Long> alreadyIncluded = result.stream()
                    .map(ProblemListItemResponse::getProbId)
                    .collect(java.util.stream.Collectors.toSet());
            List<Problem> popular = problemRepository.findPopularUnsolvedProblems(userId);
            for (Problem p : popular) {
                if (result.size() >= 3) break;
                if (!alreadyIncluded.contains(p.getId())) {
                    result.add(ProblemListItemResponse.of(p, solvedIds.contains(p.getId())));
                }
            }
        }

        String reason = topCategory + " 카테고리를 자주 틀리셨어요";
        return new ProblemRecommendResponse(reason, result);
    }

    private ProblemRecommendResponse fallback(Long userId, Set<Long> solvedIds) {
        List<Problem> popular = problemRepository.findPopularUnsolvedProblems(userId);
        List<ProblemListItemResponse> result = popular.stream()
                .map(p -> ProblemListItemResponse.of(p, solvedIds.contains(p.getId())))
                .toList();
        return new ProblemRecommendResponse("많은 사람들이 푼 문제를 추천해드려요", result);
    }
}

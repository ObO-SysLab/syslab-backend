package net.diveon.backend.domain.grade.service;

import net.diveon.backend.domain.grade.dto.response.GradeBoardObjectiveResponse;
import net.diveon.backend.domain.grade.dto.response.GradeBoardPracticeResponse;
import net.diveon.backend.domain.grade.dto.response.interfaces.GradeBoardResponse;
import net.diveon.backend.domain.grade.entity.SolveSubmission;
import net.diveon.backend.domain.grade.entity.SovleResult;
import net.diveon.backend.domain.grade.repository.SolveResultRepository;
import net.diveon.backend.domain.grade.repository.SolveSubmissionRepository;
import net.diveon.backend.domain.problem.entity.Problem;
import net.diveon.backend.domain.problem.repository.ProblemRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class GradeBoardService {

    private final SolveSubmissionRepository solveSubmissionRepository;
    private final SolveResultRepository solveResultRepository;
    private final ProblemRepository problemRepository;

    public GradeBoardService(SolveSubmissionRepository solveSubmissionRepository,
                             SolveResultRepository solveResultRepository,
                             ProblemRepository problemRepository) {
        this.solveSubmissionRepository = solveSubmissionRepository;
        this.solveResultRepository = solveResultRepository;
        this.problemRepository = problemRepository;
    }

    public GradeBoardResponse getBoard(Long probId, int page, int size, String result, String sort) {
        Problem problem = problemRepository.findById(probId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 문제입니다."));

        Sort sortOption = sort.equals("oldest")
                ? Sort.by("submittedAt").ascending()
                : Sort.by("submittedAt").descending();

        Pageable pageable = PageRequest.of(page - 1, size, sortOption);

        String type = problem.getType();

        if (type.equals("objective")) {
            return buildObjectiveBoard(probId, pageable, result);
        } else if (type.equals("practice")) {
            return buildPracticeBoard(probId, pageable, result);
        }

        throw new RuntimeException("지원하지 않는 문제 유형입니다.");
    }

    // 객, 코, 실 유형별로 응답 필드가 달라질 수 있어 메소드 분리

    // 객관식

    private GradeBoardObjectiveResponse buildObjectiveBoard(Long probId, Pageable pageable, String result) {
        Page<SolveSubmission> submissionPage = solveSubmissionRepository.findCompletedByProbId(probId, pageable);
        Long total = solveSubmissionRepository.countCompletedByProbId(probId);

        List<GradeBoardObjectiveResponse.SubmissionItem> items = submissionPage.getContent().stream()
                .map(submission -> {
                    SovleResult solveResult = solveResultRepository.findBySubmissionId(submission.getId()).orElse(null);

                    Boolean isCorrect = null;
                    Short score = null;
                    String judgedAt = null;

                    if (solveResult != null) {
                        isCorrect = solveResult.getResultState() == SovleResult.SovleResultState.CORRECT;
                        score = solveResult.getScore();
                        judgedAt = solveResult.getCreatedAt().toString();
                    }

                    if (!result.equals("all")) {
                        boolean wantCorrect = result.equals("correct");
                        if (isCorrect == null || isCorrect != wantCorrect) return null;
                    }

                    return new GradeBoardObjectiveResponse.SubmissionItem(
                            submission.getId(),
                            submission.getUser().getNickname(),
                            isCorrect,
                            score,
                            submission.getSubmittedAt().toString(),
                            judgedAt
                    );
                })
                .filter(item -> item != null)
                .toList();

        return new GradeBoardObjectiveResponse(probId, total, items);
    }

    // 실습형

    private GradeBoardPracticeResponse buildPracticeBoard(Long probId, Pageable pageable, String result) {
        Page<SolveSubmission> submissionPage = solveSubmissionRepository.findCompletedByProbId(probId, pageable);
        Long total = solveSubmissionRepository.countCompletedByProbId(probId);

        List<GradeBoardPracticeResponse.SubmissionItem> items = submissionPage.getContent().stream()
                .map(submission -> {
                    SovleResult solveResult = solveResultRepository.findBySubmissionId(submission.getId()).orElse(null);

                    Boolean isCorrect = null;
                    Short score = null;
                    String judgedAt = null;

                    if (solveResult != null) {
                        isCorrect = solveResult.getResultState() == SovleResult.SovleResultState.CORRECT;
                        score = solveResult.getScore();
                        judgedAt = solveResult.getCreatedAt().toString();
                    }

                    if (!result.equals("all")) {
                        boolean wantCorrect = result.equals("correct");
                        if (isCorrect == null || isCorrect != wantCorrect) return null;
                    }

                    return new GradeBoardPracticeResponse.SubmissionItem(
                            submission.getId(),
                            submission.getUser().getNickname(),
                            isCorrect,
                            score,
                            submission.getSubmittedAt().toString(),
                            judgedAt
                    );
                })
                .filter(item -> item != null)
                .toList();

        return new GradeBoardPracticeResponse(probId, total, items);
    }
}

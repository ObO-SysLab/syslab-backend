package net.diveon.backend.domain.grade.service;

import net.diveon.backend.domain.grade.dto.response.GradeBoardObjectiveResponse;
import net.diveon.backend.domain.grade.entity.SolveSubmission;
import net.diveon.backend.domain.grade.entity.SovleResult;
import net.diveon.backend.domain.grade.repository.SolveResultRepository;
import net.diveon.backend.domain.grade.repository.SolveSubmissionRepository;
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

    public GradeBoardObjectiveResponse getObjectiveBoard(Long probId, int page, int size, String result, String sort) {
        problemRepository.findById(probId).orElseThrow(() -> new RuntimeException("존재하지 않는 문제입니다."));

        Sort sortOption = sort.equals("oldest")
                ? Sort.by("submittedAt").ascending() // 오래된 것부터
                : Sort.by("submittedAt").descending(); // 최신 것부터

        Pageable pageable = PageRequest.of(page - 1, size, sortOption); // 1페이지부터 시작인데 Spring은 0부터 시작이라 -1, 한 페이지에 기본 20개씩 보여줌
        Page<SolveSubmission> submissionPage = solveSubmissionRepository.findCompletedByProbId(probId, pageable); // prob_id=42(예시), COMPLETED 상태인 제출들을 페이지 단위로 가져옴
        Long total = solveSubmissionRepository.countCompletedByProbId(probId); // 전체 제출이 몇 건인지 카운트

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
                        if (isCorrect == null || isCorrect != wantCorrect) return null; // 둘 중 하나라도 해당되면 null 반환 -> 목록에서 제외
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
                .filter(item -> item != null) // null 제외
                .toList(); // 리스트로 반환
                
        // prob_id, 전체 건수, 제출 목록 조합해서 반환
        return new GradeBoardObjectiveResponse(probId, total, items);
    }
}
package net.diveon.backend.domain.grade.service;

import net.diveon.backend.domain.grade.dto.response.GradeBoardCodingResponse;
import net.diveon.backend.domain.grade.dto.response.GradeBoardObjectiveResponse;
import net.diveon.backend.domain.grade.dto.response.GradeBoardPracticeResponse;
import net.diveon.backend.domain.grade.dto.response.interfaces.GradeBoardResponse;
import net.diveon.backend.domain.grade.entity.SolveSubmission;
import net.diveon.backend.domain.grade.entity.SovleResult;
import net.diveon.backend.domain.grade.entity.SovleResultCoding;
import net.diveon.backend.domain.grade.entity.SolveSubmissionCoding;
import net.diveon.backend.domain.grade.repository.SolveResultRepository;
import net.diveon.backend.domain.grade.repository.SolveSubmissionCodingRepository;
import net.diveon.backend.domain.grade.repository.SolveSubmissionRepository;
import net.diveon.backend.domain.grade.repository.SovleResultCodingRepository;
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
    private final SolveSubmissionCodingRepository solveSubmissionCodingRepository;
    private final SolveResultRepository solveResultRepository;
    private final SovleResultCodingRepository solveResultCodingRepository;
    private final ProblemRepository problemRepository;

    public GradeBoardService(SolveSubmissionRepository solveSubmissionRepository,
                             SolveSubmissionCodingRepository solveSubmissionCodingRepository,
                             SolveResultRepository solveResultRepository,
                             SovleResultCodingRepository solveResultCodingRepository,
                             ProblemRepository problemRepository) {
        this.solveSubmissionRepository = solveSubmissionRepository;
        this.solveSubmissionCodingRepository = solveSubmissionCodingRepository;
        this.solveResultRepository = solveResultRepository;
        this.solveResultCodingRepository = solveResultCodingRepository;
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


        // 유형별 분기
        if (type.equals("objective")) {
            return buildObjectiveBoard(probId, pageable, result);
        } else if (type.equals("practice")) {
            return buildPracticeBoard(probId, pageable, result);
        } else if (type.equals("coding")) {
            return buildCodingBoard(probId, pageable, result);
        }

        throw new RuntimeException("지원하지 않는 문제 유형입니다.");
    }

    // 객관식, 코딩형, 실습형 유형별로 응답 필드가 달라질 수 있어 메소드 분리한 것.

    // 객관식
    private GradeBoardObjectiveResponse buildObjectiveBoard(Long probId, Pageable pageable, String result) {
        Page<SolveSubmission> submissionPage = solveSubmissionRepository.findCompletedByProbId(probId, pageable);
        Long total = solveSubmissionRepository.countCompletedByProbId(probId);

        List<GradeBoardObjectiveResponse.SubmissionItem> items = submissionPage.getContent().stream()
                .map(submission -> {
                    SovleResult solveResult = solveResultRepository.findBySubmissionId(submission.getId()).orElse(null);

                    Boolean isCorrect = null;
                    String judgedAt = null;

                    if (solveResult != null) {
                        isCorrect = solveResult.getResultState() == SovleResult.SovleResultState.CORRECT;
                        judgedAt = solveResult.getCreatedAt().toString();
                    }

                    // 채점 보드에서 정답/오답 필터링하는 로직 (프론트에서 정답만 보기, 오답만 보기 선택했을때 필터링)
                    if (!result.equals("all")) {
                        boolean wantCorrect = result.equals("correct");
                        if (isCorrect == null || isCorrect != wantCorrect) return null;
                    }

                    return new GradeBoardObjectiveResponse.SubmissionItem(
                            submission.getId(),
                            submission.getUser().getNickname(),
                            isCorrect,
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
                    String judgedAt = null;

                    if (solveResult != null) {
                        isCorrect = solveResult.getResultState() == SovleResult.SovleResultState.CORRECT;
                        judgedAt = solveResult.getCreatedAt().toString();
                    }

                    // 채점 보드에서 정답/오답 필터링하는 로직 (프론트에서 정답만 보기, 오답만 보기 선택했을때 필터링)
                    if (!result.equals("all")) {
                        boolean wantCorrect = result.equals("correct");
                        if (isCorrect == null || isCorrect != wantCorrect) return null;
                    }

                    return new GradeBoardPracticeResponse.SubmissionItem(
                            submission.getId(),
                            submission.getUser().getNickname(),
                            isCorrect,
                            submission.getSubmittedAt().toString(),
                            judgedAt
                    );
                })
                .filter(item -> item != null)
                .toList();

        return new GradeBoardPracticeResponse(probId, total, items);
    }

    // 코딩형
    private GradeBoardCodingResponse buildCodingBoard(Long probId, Pageable pageable, String result) {
        Page<SolveSubmission> submissionPage = solveSubmissionRepository.findCompletedByProbId(probId, pageable);
        Long total = solveSubmissionRepository.countCompletedByProbId(probId);

        List<GradeBoardCodingResponse.SubmissionItem> items = submissionPage.getContent().stream()
                .map(submission -> {
                    SovleResult solveResult = solveResultRepository.findBySubmissionId(submission.getId()).orElse(null);
                    SolveSubmissionCoding submissionCoding = solveSubmissionCodingRepository.findById(submission.getId()).orElse(null);

                    Boolean isCorrect = null;
                    String judgedAt = null;
                    Short score = null;
                    Integer runtime = null;
                    Integer memoryUsage = null;
                    Integer codeSize = null;
                    String language = submissionCoding != null ? submissionCoding.getLanguage() : null;

                    if (solveResult != null) {
                        isCorrect = solveResult.getResultState() == SovleResult.SovleResultState.CORRECT; // CORRECT면 true
                        judgedAt = solveResult.getCreatedAt().toString(); // 채점 완료 시각

                        SovleResultCoding codingResult = solveResultCodingRepository.findById(solveResult.getId()).orElse(null);
                        if (codingResult != null) {
                            score = codingResult.getScore();
                            runtime = codingResult.getRuntime();
                            memoryUsage = codingResult.getMemoryUsage();
                            codeSize = codingResult.getCodeSize();
                        }
                    }

                    // 채점 보드에서 정답/오답 필터링하는 로직 (프론트에서 정답만 보기, 오답만 보기 선택했을때 필터링)
                    if (!result.equals("all")) {
                        boolean wantCorrect = result.equals("correct");
                        if (isCorrect == null || isCorrect != wantCorrect) return null;
                    }

                    return new GradeBoardCodingResponse.SubmissionItem(
                            submission.getId(),
                            submission.getUser().getNickname(),
                            language,
                            isCorrect,
                            score,
                            runtime,
                            memoryUsage,
                            codeSize,
                            submission.getSubmittedAt().toString(),
                            judgedAt
                    );
                })
                .filter(item -> item != null) // null 제거 후 리스트로
                .toList();

        return new GradeBoardCodingResponse(probId, total, items);
    }
}

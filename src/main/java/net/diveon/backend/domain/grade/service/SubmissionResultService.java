package net.diveon.backend.domain.grade.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.diveon.backend.domain.grade.dto.response.SubmissionResultCodingResponse;
import net.diveon.backend.domain.grade.dto.response.SubmissionResultObjectiveResponse;
import net.diveon.backend.domain.grade.dto.response.SubmissionResultPracticeResponse;
import net.diveon.backend.domain.grade.dto.response.SubmissionResultResponse;
import net.diveon.backend.domain.grade.entity.SolveResult;
import net.diveon.backend.domain.grade.entity.SolveResultCoding;
import net.diveon.backend.domain.grade.entity.SolveSubmission;
import net.diveon.backend.domain.grade.entity.SolveSubmissionCoding;
import net.diveon.backend.domain.grade.entity.SolveSubmissionObjective;
import net.diveon.backend.domain.grade.repository.SolveResultCodingRepository;
import net.diveon.backend.domain.grade.repository.SolveResultRepository;
import net.diveon.backend.domain.grade.repository.SolveSubmissionCodingRepository;
import net.diveon.backend.domain.grade.repository.SolveSubmissionObjectiveRepository;
import net.diveon.backend.domain.grade.repository.SolveSubmissionRepository;
import net.diveon.backend.global.exception.InvalidProblemTypeException;
import net.diveon.backend.global.exception.SubmissionAccessDeniedException;
import net.diveon.backend.global.exception.SubmissionNotFoundException;
import net.diveon.backend.global.exception.SubmissionNotCompletedException;


// 상세 채점 결과 조회
@Service
public class SubmissionResultService {

    private final SolveSubmissionRepository solveSubmissionRepository;
    private final SolveResultRepository solveResultRepository;
    private final SolveSubmissionObjectiveRepository solveSubmissionObjectiveRepository;
    private final SolveSubmissionCodingRepository solveSubmissionCodingRepository;
    private final SolveResultCodingRepository solveResultCodingRepository;

    public SubmissionResultService(
        SolveSubmissionRepository solveSubmissionRepository,
        SolveResultRepository solveResultRepository,
        SolveSubmissionObjectiveRepository solveSubmissionObjectiveRepository,
        SolveSubmissionCodingRepository solveSubmissionCodingRepository,
        SolveResultCodingRepository solveResultCodingRepository
    ) {
        this.solveSubmissionRepository = solveSubmissionRepository;
        this.solveResultRepository = solveResultRepository;
        this.solveSubmissionObjectiveRepository = solveSubmissionObjectiveRepository;
        this.solveSubmissionCodingRepository = solveSubmissionCodingRepository;
        this.solveResultCodingRepository = solveResultCodingRepository;
    }

    @Transactional(readOnly = true)
    public SubmissionResultResponse getResult(long userId, long submissionId) {

        // 1. 제출 조회 - 없으면 404
        SolveSubmission submission = solveSubmissionRepository.findById(submissionId)
            .orElseThrow(() -> new SubmissionNotFoundException("존재하지 않는 제출입니다."));

        // 2. 본인 제출인지 확인 - 아니면 403
        if (!submission.getUser().getId().equals(userId)) {
            throw new SubmissionAccessDeniedException("본인의 제출만 조회할 수 있습니다.");
        }

        // 3. 채점 완료 확인 - 아니면 409
        if (submission.getSubmissionState() != SolveSubmission.SubmissionState.COMPLETED) {
            throw new SubmissionNotCompletedException("아직 채점이 완료되지 않은 제출입니다.");
        }

        // 4. 채점 결과 조회
        SolveResult result = solveResultRepository.findBySubmissionId(submissionId)
            .orElseThrow(() -> new SubmissionNotFoundException("채점 결과가 존재하지 않습니다."));

        boolean isCorrect = result.getResultState() == SolveResult.SolveResultState.CORRECT;
        String probType = submission.getProblem().getType();

        // 5. probType에 따라 응답 빌드
        return switch (probType) {
            case "objective" -> buildObjectiveResponse(submission, result, isCorrect);
            case "coding"    -> buildCodingResponse(submission, result, isCorrect);
            case "practice"  -> buildPracticeResponse(submission, result, isCorrect);
            default -> throw new InvalidProblemTypeException("유효하지 않은 문제 유형입니다: " + probType);
        };
    }

    // 객관식
    private SubmissionResultObjectiveResponse buildObjectiveResponse(
        SolveSubmission submission, SolveResult result, boolean isCorrect
    ) {
        SolveSubmissionObjective submissionObjective = solveSubmissionObjectiveRepository
            .findById(submission.getId())
            .orElseThrow(() -> new SubmissionNotFoundException("객관식 제출 정보가 존재하지 않습니다."));

        return new SubmissionResultObjectiveResponse(
            submission.getId(),
            submission.getProblem().getId(),
            isCorrect,
            submissionObjective.getAnswer(),
            submission.getSubmittedAt(),
            result.getCreatedAt()
        );
    }

    // 코딩형
    private SubmissionResultCodingResponse buildCodingResponse(
        SolveSubmission submission, SolveResult result, boolean isCorrect
    ) {
        SolveSubmissionCoding submissionCoding = solveSubmissionCodingRepository
            .findById(submission.getId())
            .orElseThrow(() -> new SubmissionNotFoundException("코딩형 제출 정보가 존재하지 않습니다."));

        SolveResultCoding resultCoding = solveResultCodingRepository
            .findById(result.getId())
            .orElseThrow(() -> new SubmissionNotFoundException("코딩형 채점 결과가 존재하지 않습니다."));

        return new SubmissionResultCodingResponse(
            submission.getId(),
            submission.getProblem().getId(),
            submissionCoding.getLanguage(),
            isCorrect,
            resultCoding.getScore(),
            resultCoding.getRuntime(),
            resultCoding.getMemoryUsage(),
            resultCoding.getCodeSize(),
            submission.getSubmittedAt(),
            result.getCreatedAt(),
            submissionCoding.getAnswer()
        );
    }

    // 실습형
    private SubmissionResultPracticeResponse buildPracticeResponse(
        SolveSubmission submission, SolveResult result, boolean isCorrect
    ) {
        return new SubmissionResultPracticeResponse(
            submission.getId(),
            submission.getProblem().getId(),
            isCorrect,
            submission.getSubmittedAt(),
            result.getCreatedAt()
        );
    }
}

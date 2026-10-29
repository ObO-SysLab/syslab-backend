package net.diveon.backend.domain.grade.service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.diveon.backend.domain.contest.entity.ContestParticipant;
import net.diveon.backend.domain.contest.entity.ContestSubmission;
import net.diveon.backend.domain.contest.repository.ContestParticipantRepository;
import net.diveon.backend.domain.contest.repository.ContestSubmissionRepository;
import net.diveon.backend.domain.grade.dto.message.CodingGradeMessage;
import net.diveon.backend.domain.grade.entity.SolveResult;
import net.diveon.backend.domain.grade.entity.SolveResult.SolveResultState;
import net.diveon.backend.domain.grade.entity.SolveResultCoding;
import net.diveon.backend.domain.grade.entity.SolveSubmission;
import net.diveon.backend.domain.grade.entity.SolveSubmission.SubmissionState;
import net.diveon.backend.domain.grade.entity.SolveSubmissionCoding;
import net.diveon.backend.domain.grade.repository.SolveResultCodingRepository;
import net.diveon.backend.domain.grade.repository.SolveResultRepository;
import net.diveon.backend.domain.grade.repository.SolveSubmissionCodingRepository;
import net.diveon.backend.domain.grade.repository.SolveSubmissionRepository;

@Service
@Profile("local")
@Transactional
public class CodingGradeQueueServiceMock implements CodingGradeQueueService {

    private static final Logger log = LoggerFactory.getLogger(CodingGradeQueueServiceMock.class);

    private final SolveSubmissionRepository solveSubmissionRepository;
    private final SolveSubmissionCodingRepository solveSubmissionCodingRepository;
    private final SolveResultRepository solveResultRepository;
    private final SolveResultCodingRepository solveResultCodingRepository;
    private final ContestSubmissionRepository contestSubmissionRepository;
    private final ContestParticipantRepository contestParticipantRepository;
    private final net.diveon.backend.domain.problem.repository.ProblemRepository problems;

    public CodingGradeQueueServiceMock(
        SolveSubmissionRepository solveSubmissionRepository,
        SolveSubmissionCodingRepository solveSubmissionCodingRepository,
        SolveResultRepository solveResultRepository,
        SolveResultCodingRepository solveResultCodingRepository,
        ContestSubmissionRepository contestSubmissionRepository,
        ContestParticipantRepository contestParticipantRepository,
        StringRedisTemplate redisTemplate,
        net.diveon.backend.domain.problem.repository.ProblemRepository problems
    ) {
        this.solveSubmissionRepository = solveSubmissionRepository;
        this.solveSubmissionCodingRepository = solveSubmissionCodingRepository;
        this.solveResultRepository = solveResultRepository;
        this.solveResultCodingRepository = solveResultCodingRepository;
        this.contestSubmissionRepository = contestSubmissionRepository;
        this.contestParticipantRepository = contestParticipantRepository;
        this.problems = problems;
    }

    @Override
    public void sendContestGradeRequest(long probId, long submitterId, long submissionId,
                                        CodingGradeMessage.ContestContext contestContext) {
        SolveSubmissionCoding submissionCoding = solveSubmissionCodingRepository.findById(submissionId)
            .orElseThrow(() -> new RuntimeException("코딩형 제출 정보가 없습니다."));

        log.info(
            "[Mock] 대회 코딩 채점 요청 생략 - probId: {}, submitterId: {}, submissionId: {}, contestId: {}",
            probId, submitterId, submissionId, contestContext.getContestId()
        );

        var lockedParticipant = contestParticipantRepository.findForScoreUpdate(contestContext.getContestId(), submitterId)
                .orElseThrow(() -> new IllegalStateException("Contest participant not found"));

        // ① 일반 채점 처리 (solve_submission + solve_result + solve_result_coding)
        sendGradeRequest(probId, submitterId, submissionId);

        // ② contest_submission 업데이트
        ContestSubmission contestSubmission = contestSubmissionRepository
            .findBySolveSubmissionId(submissionId)
            .orElse(null);
        if (contestSubmission == null) {
            log.warn("[Mock] contest_submission을 찾을 수 없음 - submissionId: {}", submissionId);
            return;
        }

        boolean isCorrect = getMockResultState(submissionCoding.getLanguage()) == SolveResultState.CORRECT;

        // ③ 첫 정답 여부 확인 (업데이트 전에 확인)
        List<Long> solvedProblemIds = contestSubmissionRepository
            .findSolvedContestProblemIds(contestContext.getContestId(), submitterId);
        boolean alreadySolved = solvedProblemIds.contains(contestContext.getContestProblemId());

        contestSubmission.updateResultAndStatus(isCorrect, "COMPLETED");

        // ④ 첫 정답이면 점수 반영 + Redis 갱신
        if (isCorrect && !alreadySolved) {
            lockedParticipant.addScore(contestContext.getPoints(), LocalDateTime.now(java.time.ZoneOffset.UTC));
        }
    }

    @Override
    public void sendGradeRequest(long probId, long submitterId, long submissionId) {
        SolveSubmission submission = solveSubmissionRepository.findById(submissionId)
            .orElseThrow(() -> new RuntimeException("제출 정보가 없습니다."));
        SolveSubmissionCoding submissionCoding = solveSubmissionCodingRepository.findById(submissionId)
            .orElseThrow(() -> new RuntimeException("코딩형 제출 정보가 없습니다."));

        log.info(
            "[Mock] 코딩 채점 요청 생략 - probId: {}, submitterId: {}, submissionId: {}, language: {}",
            probId, submitterId, submissionId, submissionCoding.getLanguage()
        );

        SolveResultState resultState = getMockResultState(submissionCoding.getLanguage());
        if (submission.getSubmissionState() == SubmissionState.COMPLETED || submission.getSubmissionState() == SubmissionState.FAILED) return;
        SolveResult result = new SolveResult(submission, resultState);
        solveResultRepository.save(result);

        SolveResultCoding resultCoding = new SolveResultCoding(
            result,
            getMockScore(resultState),
            0,
            0,
            submissionCoding.getAnswer().length()
        );
        solveResultCodingRepository.save(resultCoding);

        if (resultState == SolveResultState.CORRECT) problems.recordSolver(probId, submitterId);
        submission.setSubmissionState(SubmissionState.COMPLETED);
    }

    private SolveResultState getMockResultState(String language) {
        if ("python".equals(language)) {
            return SolveResultState.CORRECT;
        }
        return SolveResultState.WRONG;
    }

    private Short getMockScore(SolveResultState resultState) {
        return resultState == SolveResultState.CORRECT ? (short) 100 : (short) 0;
    }
}

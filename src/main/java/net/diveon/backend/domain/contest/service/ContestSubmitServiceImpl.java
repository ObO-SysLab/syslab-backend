package net.diveon.backend.domain.contest.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.diveon.backend.domain.contest.dto.request.ContestSubmitRequest;
import net.diveon.backend.domain.contest.dto.response.ContestSubmitAsyncResponse;
import net.diveon.backend.domain.contest.dto.response.ContestSubmitImmediateResponse;
import net.diveon.backend.domain.contest.entity.Contest;
import net.diveon.backend.domain.contest.entity.ContestParticipant;
import net.diveon.backend.domain.contest.entity.ContestProblem;
import net.diveon.backend.domain.contest.entity.ContestSubmission;
import net.diveon.backend.domain.contest.repository.ContestParticipantRepository;
import net.diveon.backend.domain.contest.repository.ContestProblemRepository;
import net.diveon.backend.domain.contest.repository.ContestRepository;
import net.diveon.backend.domain.contest.repository.ContestSubmissionRepository;
import net.diveon.backend.domain.problem.entity.Problem;
import net.diveon.backend.domain.user.entity.User;
import net.diveon.backend.domain.user.repository.UserRepository;
import net.diveon.backend.global.exception.ContestNotOngoingException;
import net.diveon.backend.global.exception.ContestNotFoundException;
import net.diveon.backend.global.exception.ContestParticipantNotFoundException;
import net.diveon.backend.global.exception.ContestProblemNotFoundException;
import net.diveon.backend.global.exception.UserNotFoundException;
import net.diveon.backend.global.response.ApiResponse;

@Service
public class ContestSubmitServiceImpl implements ContestSubmitService {

    private final ContestRepository contestRepository;
    private final ContestProblemRepository contestProblemRepository;
    private final ContestParticipantRepository contestParticipantRepository;
    private final ContestSubmissionRepository contestSubmissionRepository;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public ContestSubmitServiceImpl(
            ContestRepository contestRepository,
            ContestProblemRepository contestProblemRepository,
            ContestParticipantRepository contestParticipantRepository,
            ContestSubmissionRepository contestSubmissionRepository,
            UserRepository userRepository,
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper) {
        this.contestRepository = contestRepository;
        this.contestProblemRepository = contestProblemRepository;
        this.contestParticipantRepository = contestParticipantRepository;
        this.contestSubmissionRepository = contestSubmissionRepository;
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<?>> submitContestProblem(
            Long contestId, Long contestProblemId, Long userId, ContestSubmitRequest request) {

        Contest contest = contestRepository.findById(contestId)
                .orElseThrow(ContestNotFoundException::new);

        if (contest.getStatus() != Contest.ContestStatus.ONGOING) {
            throw new ContestNotOngoingException();
        }

        ContestParticipant participant = contestParticipantRepository
                .findByContestIdAndUserId(contestId, userId)
                .orElseThrow(ContestParticipantNotFoundException::new);

        if (participant.getIsBanned()) {
            throw new IllegalArgumentException("밴된 사용자는 제출할 수 없습니다.");
        }

        ContestProblem contestProblem = contestProblemRepository.findById(contestProblemId)
                .orElseThrow(ContestProblemNotFoundException::new);

        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        Problem problem = contestProblem.getProblem();

        // TODO: 쿨다운 체크 (Redis)

        switch (request.getSubmissionType()) {
            case OBJECTIVE:
                return handleObjectiveSubmission(contest, contestProblem, problem, user, request);
            case PRACTICE:
                return handlePracticeSubmission(contest, contestProblem, problem, user, request);
            case CODING:
                return handleCodingSubmission(contest, contestProblem, problem, user, request);
            default:
                throw new IllegalArgumentException("잘못된 제출 유형입니다.");
        }
    }

    @Transactional
    private ResponseEntity<ApiResponse<?>> handleObjectiveSubmission(
            Contest contest, ContestProblem contestProblem, Problem problem, User user, ContestSubmitRequest request) {

        String answerJson = jdbcTemplate.queryForObject(
                "SELECT answer::text FROM problem_objective WHERE prob_id = ?",
                String.class, problem.getId());

        if (answerJson == null) {
            throw new RuntimeException("객관식 문제 정보를 찾을 수 없습니다.");
        }

        List<Integer> correctAnswers;
        try {
            correctAnswers = objectMapper.readValue(answerJson, new TypeReference<List<Integer>>() {});
        } catch (Exception e) {
            throw new RuntimeException("정답 데이터 파싱 실패: " + e.getMessage(), e);
        }

        String userAnswer = request.getAnswer();
        if (userAnswer == null || userAnswer.isEmpty()) {
            throw new IllegalArgumentException("답을 입력해주세요.");
        }

        boolean isCorrect = false;
        try {
            int userAnswerNum = Integer.parseInt(userAnswer);
            isCorrect = correctAnswers.contains(userAnswerNum);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("유효한 숫자를 입력해주세요.");
        }

        int scoreToAdd = 0;

        if (isCorrect) {
            List<Long> solvedProblems = contestSubmissionRepository
                    .findSolvedContestProblemIds(contest.getId(), user.getId());

            if (!solvedProblems.contains(contestProblem.getId())) {
                scoreToAdd = contestProblem.getPoints();
                LocalDateTime now = LocalDateTime.now();
                ContestParticipant participant = contestParticipantRepository
                        .findByContestIdAndUserId(contest.getId(), user.getId())
                        .get();
                participant.addScore(scoreToAdd, now);
                contestParticipantRepository.save(participant);

                jdbcTemplate.update("UPDATE problem_summary SET solved_count = solved_count + 1 WHERE id = ?", problem.getId());
                // TODO: Redis 스코어보드 업데이트
            }
        }

        ContestSubmission submission = new ContestSubmission(contest, contestProblem, user);
        submission.updateResult(isCorrect);
        contestSubmissionRepository.save(submission);

        jdbcTemplate.update("UPDATE problem_summary SET submitted_count = submitted_count + 1 WHERE id = ?", problem.getId());
        // TODO: Redis 쿨다운 설정 (TTL 30s)

        ContestSubmitImmediateResponse response = new ContestSubmitImmediateResponse(isCorrect, scoreToAdd);
        return ResponseEntity.ok(ApiResponse.success("제출이 완료되었습니다.", response));
    }

    @Transactional
    private ResponseEntity<ApiResponse<?>> handlePracticeSubmission(
            Contest contest, ContestProblem contestProblem, Problem problem, User user, ContestSubmitRequest request) {

        String flagHash = jdbcTemplate.queryForObject(
                "SELECT flag_hash FROM problem_practice WHERE prob_id = ?",
                String.class, problem.getId());

        if (flagHash == null) {
            throw new RuntimeException("실습 문제 정보를 찾을 수 없습니다.");
        }

        String userAnswer = request.getAnswer();
        if (userAnswer == null || userAnswer.isEmpty()) {
            throw new IllegalArgumentException("플래그를 입력해주세요.");
        }

        String userAnswerHash = hashFlag(userAnswer);
        boolean isCorrect = userAnswerHash.equals(flagHash);

        int scoreToAdd = 0;

        if (isCorrect) {
            List<Long> solvedProblems = contestSubmissionRepository
                    .findSolvedContestProblemIds(contest.getId(), user.getId());

            if (!solvedProblems.contains(contestProblem.getId())) {
                scoreToAdd = contestProblem.getPoints();
                LocalDateTime now = LocalDateTime.now();
                ContestParticipant participant = contestParticipantRepository
                        .findByContestIdAndUserId(contest.getId(), user.getId())
                        .get();
                participant.addScore(scoreToAdd, now);
                contestParticipantRepository.save(participant);

                jdbcTemplate.update("UPDATE problem_summary SET solved_count = solved_count + 1 WHERE id = ?", problem.getId());
                // TODO: Redis 스코어보드 업데이트
            }
        }

        ContestSubmission submission = new ContestSubmission(contest, contestProblem, user);
        submission.updateResult(isCorrect);
        contestSubmissionRepository.save(submission);

        jdbcTemplate.update("UPDATE problem_summary SET submitted_count = submitted_count + 1 WHERE id = ?", problem.getId());
        // TODO: Redis 쿨다운 설정 (TTL 30s)

        ContestSubmitImmediateResponse response = new ContestSubmitImmediateResponse(isCorrect, scoreToAdd);
        return ResponseEntity.ok(ApiResponse.success("제출이 완료되었습니다.", response));
    }

    @Transactional
    private ResponseEntity<ApiResponse<?>> handleCodingSubmission(
            Contest contest, ContestProblem contestProblem, Problem problem, User user, ContestSubmitRequest request) {

        ContestSubmission submission = new ContestSubmission(contest, contestProblem, user, "PENDING");
        ContestSubmission savedSubmission = contestSubmissionRepository.save(submission);

        jdbcTemplate.update("UPDATE problem_summary SET submitted_count = submitted_count + 1 WHERE id = ?", problem.getId());
        // TODO: S3 코드 업로드
        // TODO: SQS 전송 (contestContext 포함)
        // TODO: Redis 쿨다운 설정 (TTL 30s)

        ContestSubmitAsyncResponse response = new ContestSubmitAsyncResponse(
                savedSubmission.getId(), "PENDING");
        return ResponseEntity.status(202).body(
                ApiResponse.success("채점 요청이 접수되었습니다.", response));
    }

    private String hashFlag(String flag) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(flag.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 알고리즘을 찾을 수 없습니다.", e);
        }
    }
}

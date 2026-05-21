package net.diveon.backend.domain.contest.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
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
import net.diveon.backend.domain.grade.dto.message.CodingGradeMessage;
import net.diveon.backend.domain.grade.entity.SolveSubmission;
import net.diveon.backend.domain.grade.entity.SolveSubmissionCoding;
import net.diveon.backend.domain.grade.repository.SolveSubmissionCodingRepository;
import net.diveon.backend.domain.grade.repository.SolveSubmissionRepository;
import net.diveon.backend.domain.grade.service.CodingGradeQueueService;
import net.diveon.backend.domain.problem.entity.Problem;
import net.diveon.backend.domain.user.entity.User;
import net.diveon.backend.domain.user.repository.UserRepository;
import net.diveon.backend.global.exception.ContestAccessDeniedException;
import net.diveon.backend.global.exception.ContestCooldownException;
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
    private final StringRedisTemplate redisTemplate;
    private final SolveSubmissionRepository solveSubmissionRepository;
    private final SolveSubmissionCodingRepository solveSubmissionCodingRepository;
    private final CodingGradeQueueService codingGradeQueueService;

    public ContestSubmitServiceImpl(
            ContestRepository contestRepository,
            ContestProblemRepository contestProblemRepository,
            ContestParticipantRepository contestParticipantRepository,
            ContestSubmissionRepository contestSubmissionRepository,
            UserRepository userRepository,
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            StringRedisTemplate redisTemplate,
            SolveSubmissionRepository solveSubmissionRepository,
            SolveSubmissionCodingRepository solveSubmissionCodingRepository,
            CodingGradeQueueService codingGradeQueueService) {
        this.contestRepository = contestRepository;
        this.contestProblemRepository = contestProblemRepository;
        this.contestParticipantRepository = contestParticipantRepository;
        this.contestSubmissionRepository = contestSubmissionRepository;
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
        this.solveSubmissionRepository = solveSubmissionRepository;
        this.solveSubmissionCodingRepository = solveSubmissionCodingRepository;
        this.codingGradeQueueService = codingGradeQueueService;
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
                .orElseThrow(ContestAccessDeniedException::new);

        if (participant.getIsBanned()) {
            throw new ContestAccessDeniedException();
        }

        ContestProblem contestProblem = contestProblemRepository.findById(contestProblemId)
                .orElseThrow(ContestProblemNotFoundException::new);

        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        Problem problem = contestProblem.getProblem();

        checkCooldown(contestId, userId, contestProblemId);

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
                updateScoreboard(contest.getId(), user.getId(), participant.getScore(), now);
            }
        }

        ContestSubmission submission = new ContestSubmission(contest, contestProblem, user);
        submission.updateResult(isCorrect);
        contestSubmissionRepository.save(submission);

        jdbcTemplate.update("UPDATE problem_summary SET submitted_count = submitted_count + 1 WHERE id = ?", problem.getId());
        setCooldown(contest.getId(), user.getId(), contestProblem.getId());

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
                updateScoreboard(contest.getId(), user.getId(), participant.getScore(), now);
            }
        }

        ContestSubmission submission = new ContestSubmission(contest, contestProblem, user);
        submission.updateResult(isCorrect);
        contestSubmissionRepository.save(submission);

        jdbcTemplate.update("UPDATE problem_summary SET submitted_count = submitted_count + 1 WHERE id = ?", problem.getId());
        setCooldown(contest.getId(), user.getId(), contestProblem.getId());

        ContestSubmitImmediateResponse response = new ContestSubmitImmediateResponse(isCorrect, scoreToAdd);
        return ResponseEntity.ok(ApiResponse.success("제출이 완료되었습니다.", response));
    }

    @Transactional
    private ResponseEntity<ApiResponse<?>> handleCodingSubmission(
            Contest contest, ContestProblem contestProblem, Problem problem, User user, ContestSubmitRequest request) {

        // solve_submission + solve_submission_coding 생성 (그레이더가 이 ID로 채점)
        SolveSubmission solveSubmission = new SolveSubmission(problem, user);
        SolveSubmission savedSolveSubmission = solveSubmissionRepository.save(solveSubmission);

        SolveSubmissionCoding coding = new SolveSubmissionCoding(savedSolveSubmission, request.getCode(), request.getLanguage());
        solveSubmissionCodingRepository.save(coding);

        // contest_submission 생성 (solve_submission 연결)
        ContestSubmission contestSubmission = new ContestSubmission(
                contest, contestProblem, user, savedSolveSubmission, "PENDING");
        ContestSubmission savedContestSubmission = contestSubmissionRepository.save(contestSubmission);

        jdbcTemplate.update("UPDATE problem_summary SET submitted_count = submitted_count + 1 WHERE id = ?", problem.getId());

        // S3 업로드 + SQS 전송 (contestContext 포함)
        CodingGradeMessage.ContestContext contestContext = new CodingGradeMessage.ContestContext(
                contest.getId(), contestProblem.getId(), contestProblem.getPoints());
        codingGradeQueueService.sendContestGradeRequest(
                problem.getId(), user.getId(), savedSolveSubmission.getId(), contestContext);

        setCooldown(contest.getId(), user.getId(), contestProblem.getId());

        ContestSubmitAsyncResponse response = new ContestSubmitAsyncResponse(
                savedContestSubmission.getId(), "PENDING");
        return ResponseEntity.status(202).body(
                ApiResponse.success("채점 요청이 접수되었습니다.", response));
    }

    private void checkCooldown(Long contestId, Long userId, Long contestProblemId) {
        String key = "contest:" + contestId + ":cooldown:" + userId + ":" + contestProblemId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            throw new ContestCooldownException();
        }
    }

    private void setCooldown(Long contestId, Long userId, Long contestProblemId) {
        String key = "contest:" + contestId + ":cooldown:" + userId + ":" + contestProblemId;
        redisTemplate.opsForValue().set(key, "1", 30, TimeUnit.SECONDS);
    }

    private void updateScoreboard(Long contestId, Long userId, int newTotalScore, LocalDateTime solvedAt) {
        long epochSecond = solvedAt.toEpochSecond(ZoneOffset.UTC);
        double compositeScore = (double) newTotalScore * 1_000_000L - epochSecond;
        redisTemplate.opsForZSet().add("scoreboard:" + contestId, String.valueOf(userId), compositeScore);
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

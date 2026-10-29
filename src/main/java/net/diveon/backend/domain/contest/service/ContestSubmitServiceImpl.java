package net.diveon.backend.domain.contest.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Locale;
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
import net.diveon.backend.domain.problem.repository.ProblemCodingRepository;
import net.diveon.backend.domain.problem.service.ProblemAccessService;
import net.diveon.backend.domain.user.entity.User;
import net.diveon.backend.domain.user.repository.UserRepository;
import net.diveon.backend.global.exception.ContestAccessDeniedException;
import net.diveon.backend.global.exception.ContestCooldownException;
import net.diveon.backend.global.exception.ContestNotOngoingException;
import net.diveon.backend.global.exception.ContestNotFoundException;
import net.diveon.backend.global.exception.ContestParticipantNotFoundException;
import net.diveon.backend.global.exception.ContestProblemNotFoundException;
import net.diveon.backend.global.exception.UserNotFoundException;
import net.diveon.backend.global.exception.InvalidSubmissionRequestException;
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
    private final ProblemCodingRepository problemCodingRepository;
    private final ProblemAccessService access;
    private final net.diveon.backend.domain.problem.repository.ProblemRepository problems;

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
            CodingGradeQueueService codingGradeQueueService,
            ProblemCodingRepository problemCodingRepository, ProblemAccessService access, net.diveon.backend.domain.problem.repository.ProblemRepository problems) {
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
        this.problemCodingRepository = problemCodingRepository;
        this.access = access;
        this.problems = problems;
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
                .findForScoreUpdate(contestId, userId)
                .orElseThrow(ContestAccessDeniedException::new);

        if (!Boolean.FALSE.equals(participant.getIsBanned())) {
            throw new ContestAccessDeniedException();
        }

        ContestProblem contestProblem = contestProblemRepository.findByIdAndContestId(contestProblemId, contestId)
                .orElseThrow(ContestProblemNotFoundException::new);

        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        Problem problem = contestProblem.getProblem();
        access.requireContestSubmission(userId, problem, contestId);

        validateSubmission(problem, request);

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

    private void validateSubmission(Problem problem, ContestSubmitRequest request) {
        if (request == null || request.getSubmissionType() == null
                || !request.getSubmissionType().name().toLowerCase(Locale.ROOT).equals(problem.getType())) {
            throw new InvalidSubmissionRequestException("문제 유형과 제출 유형이 일치하지 않습니다.");
        }
        if (request.getSubmissionType() == ContestSubmitRequest.SubmissionType.CODING) {
            String language = request.getLanguage();
            if (request.getCode() == null || request.getCode().isBlank()
                    || language == null || !List.of("python", "c", "cpp").contains(language)) {
                throw new InvalidSubmissionRequestException("코드와 지원하는 언어를 입력해주세요.");
            }
            var coding = problemCodingRepository.findById(problem.getId())
                    .orElseThrow(() -> new InvalidSubmissionRequestException("코딩형 문제가 없습니다."));
            if (coding.getAllowedLanguages() == null || !coding.getAllowedLanguages().contains(language)) {
                throw new InvalidSubmissionRequestException("이 문제에서 허용되지 않은 언어입니다.");
            }
        }
    }

    @Transactional
    private ResponseEntity<ApiResponse<?>> handleObjectiveSubmission(
            Contest contest, ContestProblem contestProblem, Problem problem, User user, ContestSubmitRequest request) {

        Map<String, Object> objectiveProblem = jdbcTemplate.queryForMap(
                "SELECT answer::text AS answer, is_seqeuntial AS is_seqeuntial FROM problem_objective WHERE prob_id = ?",
                problem.getId());

        String answerJson = (String) objectiveProblem.get("answer");
        if (answerJson == null) {
            throw new RuntimeException("객관식 문제 정보를 찾을 수 없습니다.");
        }
        Boolean isSeqeuntialAnswer = (Boolean) objectiveProblem.get("is_seqeuntial");

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

        List<Integer> submittedAnswers;
        try {
            submittedAnswers = parseObjectiveAnswer(userAnswer);
        } catch (Exception e) {
            throw new IllegalArgumentException("유효한 숫자 또는 숫자 배열을 입력해주세요.");
        }
        boolean isCorrect = isCorrectObjectiveAnswer(correctAnswers, submittedAnswers, isSeqeuntialAnswer);

        ContestParticipant participant = contestParticipantRepository
                .findByContestIdAndUserId(contest.getId(), user.getId()).get();

        if (isCorrect) {
            List<Long> solvedProblems = contestSubmissionRepository
                    .findSolvedContestProblemIds(contest.getId(), user.getId());

            if (!solvedProblems.contains(contestProblem.getId())) {
                LocalDateTime now = jdbcTemplate.queryForObject("SELECT clock_timestamp() AT TIME ZONE 'UTC'", LocalDateTime.class);
                participant.addScore(contestProblem.getPoints(), now);
                contestParticipantRepository.saveAndFlush(participant);


            }
        }

        if (isCorrect) problems.recordSolver(problem.getId(), user.getId());
        int currentRank = getRank(contest.getId(), user.getId(), participant.getScore());

        ContestSubmission submission = new ContestSubmission(contest, contestProblem, user);
        submission.updateResult(isCorrect);
        ContestSubmission savedSubmission = contestSubmissionRepository.save(submission);

        jdbcTemplate.update("UPDATE problem_summary SET submitted_count = submitted_count + 1 WHERE id = ?", problem.getId());
        setCooldown(contest.getId(), user.getId(), contestProblem.getId());

        ContestSubmitImmediateResponse response = new ContestSubmitImmediateResponse(
                savedSubmission.getId(), isCorrect, participant.getScore(), currentRank);
        return ResponseEntity.ok(ApiResponse.success("채점 요청이 접수되었습니다.", response));
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

        ContestParticipant participant = contestParticipantRepository
                .findByContestIdAndUserId(contest.getId(), user.getId()).get();

        if (isCorrect) {
            List<Long> solvedProblems = contestSubmissionRepository
                    .findSolvedContestProblemIds(contest.getId(), user.getId());

            if (!solvedProblems.contains(contestProblem.getId())) {
                LocalDateTime now = jdbcTemplate.queryForObject("SELECT clock_timestamp() AT TIME ZONE 'UTC'", LocalDateTime.class);
                participant.addScore(contestProblem.getPoints(), now);
                contestParticipantRepository.saveAndFlush(participant);


            }
        }

        if (isCorrect) problems.recordSolver(problem.getId(), user.getId());
        int currentRank = getRank(contest.getId(), user.getId(), participant.getScore());

        ContestSubmission submission = new ContestSubmission(contest, contestProblem, user);
        submission.updateResult(isCorrect);
        ContestSubmission savedSubmission = contestSubmissionRepository.save(submission);

        jdbcTemplate.update("UPDATE problem_summary SET submitted_count = submitted_count + 1 WHERE id = ?", problem.getId());
        setCooldown(contest.getId(), user.getId(), contestProblem.getId());

        ContestSubmitImmediateResponse response = new ContestSubmitImmediateResponse(
                savedSubmission.getId(), isCorrect, participant.getScore(), currentRank);
        return ResponseEntity.ok(ApiResponse.success("채점 요청이 접수되었습니다.", response));
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
                savedContestSubmission.getId(), "PENDING", savedSolveSubmission.getId());
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

    private int getRank(Long contestId, Long userId, int score) {
        Integer rank = jdbcTemplate.queryForObject(ContestRankQuery.SQL, Integer.class, contestId, userId);
        return rank == null ? 0 : rank;
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

    private List<Integer> parseObjectiveAnswer(String answer) throws Exception {
        String trimmedAnswer = answer.trim();
        if (trimmedAnswer.startsWith("[")) {
            return objectMapper.readValue(trimmedAnswer, new TypeReference<List<Integer>>() {});
        }
        return List.of(Integer.parseInt(trimmedAnswer));
    }

    private boolean isCorrectObjectiveAnswer(List<Integer> correctAnswer, List<Integer> submittedAnswer, Boolean isSeqeuntialAnswer) {
        if (correctAnswer == null || submittedAnswer == null) {
            return false;
        }

        if (Boolean.TRUE.equals(isSeqeuntialAnswer)) {
            return correctAnswer.equals(submittedAnswer);
        }

        return toAnswerCountMap(correctAnswer).equals(toAnswerCountMap(submittedAnswer));
    }

    private Map<Integer, Integer> toAnswerCountMap(List<Integer> answers) {
        Map<Integer, Integer> answerCountMap = new HashMap<>();
        for (Integer answer : answers) {
            answerCountMap.merge(answer, 1, Integer::sum);
        }
        return answerCountMap;
    }
}

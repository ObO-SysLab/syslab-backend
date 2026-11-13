package net.diveon.backend.domain.problem.service;

import net.diveon.backend.domain.contest.entity.Contest;
import net.diveon.backend.domain.contest.entity.ContestParticipant;
import net.diveon.backend.domain.contest.entity.ContestProblem;
import net.diveon.backend.domain.contest.repository.ContestParticipantRepository;
import net.diveon.backend.domain.contest.repository.ContestProblemRepository;
import net.diveon.backend.domain.contest.repository.ContestRepository;
import net.diveon.backend.global.exception.ContestAccessDeniedException;
import net.diveon.backend.domain.group.entity.Group;
import net.diveon.backend.domain.group.entity.GroupProblem;
import net.diveon.backend.domain.group.repository.GroupProblemRepository;
import net.diveon.backend.domain.group.repository.GroupRepository;
import net.diveon.backend.global.exception.ContestNotFoundException;
import net.diveon.backend.global.exception.GroupNotFoundException;
import net.diveon.backend.domain.problem.dto.request.ProblemCreateObjectiveRequest;
import net.diveon.backend.domain.problem.dto.request.ProblemCreatePracticeRequest;
import net.diveon.backend.domain.problem.dto.request.ProblemCreateCodingRequest;
import com.fasterxml.jackson.databind.JsonNode;
import net.diveon.backend.domain.problem.dto.response.ProblemCreateObjectiveResponse;
import net.diveon.backend.domain.problem.dto.response.ProblemCreatePracticeResponse;
import net.diveon.backend.domain.problem.dto.response.ProblemCreateCodingResponse;
import net.diveon.backend.domain.problem.entity.OboStep;
import net.diveon.backend.domain.problem.entity.Problem;
import net.diveon.backend.domain.problem.entity.ProblemObjective;
import net.diveon.backend.domain.problem.entity.ProblemPractice;
import net.diveon.backend.domain.problem.entity.ProblemCoding;
import net.diveon.backend.domain.problem.others.ForDtoOboStep;
import net.diveon.backend.domain.problem.repository.OboStepRepository;
import net.diveon.backend.domain.problem.repository.ProblemObjectiveRepository;
import net.diveon.backend.domain.problem.repository.ProblemPracticeRepository;
import net.diveon.backend.domain.problem.repository.ProblemCodingRepository;
import net.diveon.backend.domain.problem.repository.ProblemRepository;
import net.diveon.backend.domain.user.entity.User;
import net.diveon.backend.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

@Service
public class ProblemCreateService {
    private final ProblemObjectiveRepository problemObjectiveRepository;
    private final ProblemPracticeRepository problemPracticeRepository;
    private final OboStepRepository oboStepRepository;
    private final ProblemCodingRepository problemCodingRepository;
    private final ProblemRepository problemRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;
    private final GroupRepository groupRepository;
    private final GroupProblemRepository groupProblemRepository;
    private final ContestRepository contestRepository;
    private final ContestProblemRepository contestProblemRepository;
    private final ContestParticipantRepository contestParticipantRepository;
    private final PracticeBuildProperties practiceBuildProperties;
    private final Clock practiceBuildClock;
    private final OboDocumentValidator oboValidator;
    private final ProblemRevisionService revisions;

    public ProblemCreateService(ProblemObjectiveRepository problemObjectiveRepository,
        ProblemPracticeRepository problemPracticeRepository,
        ProblemCodingRepository problemCodingRepository,
        ProblemRepository problemRepository,
        UserRepository userRepository,
        OboStepRepository oboStepRepository,
        S3Service s3Service,
        GroupRepository groupRepository,
        GroupProblemRepository groupProblemRepository,
        ContestRepository contestRepository,
        ContestProblemRepository contestProblemRepository,
        ContestParticipantRepository contestParticipantRepository,
        PracticeBuildProperties practiceBuildProperties,
        @Qualifier("practiceBuildClock") Clock practiceBuildClock,
        OboDocumentValidator oboValidator, ProblemRevisionService revisions){
        this.problemObjectiveRepository = problemObjectiveRepository;
        this.problemPracticeRepository = problemPracticeRepository;
        this.problemCodingRepository = problemCodingRepository;
        this.problemRepository = problemRepository;
        this.userRepository = userRepository;
        this.oboStepRepository = oboStepRepository;
        this.s3Service = s3Service;
        this.groupRepository = groupRepository;
        this.groupProblemRepository = groupProblemRepository;
        this.contestRepository = contestRepository;
        this.contestProblemRepository = contestProblemRepository;
        this.contestParticipantRepository = contestParticipantRepository;
        this.practiceBuildProperties = practiceBuildProperties;
        this.practiceBuildClock = practiceBuildClock;
        this.oboValidator = oboValidator;
        this.revisions = revisions;
    }


    // 객관식형
    @Transactional
    public ProblemCreateObjectiveResponse createObjective(long userId, ProblemCreateObjectiveRequest request){
        oboValidator.validateQuestion(request.getOboJson(), "objective", request.getAnswer());
        oboValidator.validateChoices(request.getOboJson(), request.getChoices());
        User author = userRepository.findById(userId)
                .orElseThrow();

        Problem problem = new Problem(
                author,
                "objective",
                request.getTitle(),
                request.getCategory(),
                request.getDifficulty(),
                request.getVisibility()
        );
        Problem savedProblem = problemRepository.save(problem);

        Boolean oboEnabled = request.getOboEnabled() != null ? request.getOboEnabled() : false;
        ProblemObjective problemObjective = new ProblemObjective(
                savedProblem,
                request.getSummary(),
                request.getDescription(),
                request.getChoices(),
                request.getAnswer(),
                OboDocumentValidator.simulation(request.getOboJson()) == null && Boolean.TRUE.equals(request.getIsSeqeuntialAnswer()),
                oboEnabled
        );
        applyOboJson(problemObjective, request.getOboJson());
        ObjectiveAnswerRules.validate(problemObjective, request.getAnswer());
        problemObjectiveRepository.save(problemObjective);
        //TODO : 이 로직이 왜 이런건지 확인하기
        // 지금 구조가 request.getObo().getSteps() 이런식이라 
        // 이건 request를 수정하든 다른 방식을추가해야할듯. 
        if (Boolean.TRUE.equals(oboEnabled) && request.getObo() != null && request.getObo().getSteps() != null && !request.getObo().getSteps().isEmpty()) {
            List<OboStep> oboSteps = request.getObo().getSteps().stream()
                .map(step -> toOboStep(savedProblem, step))
                .toList();
            oboStepRepository.saveAll(oboSteps);
        }

        /**
         * 위 함수 설명
         * .stream() // 이 리스트를 스트림으로 바꿉니다. 즉 “각 원소를 순서대로 가공하겠다”는 뜻입니
         * 
         * .map(step -> toOboStep(savedProblem, step))
         * 각 ForDtoOboStep 원소를 OboStep으로 변환합니다.
         * 여기서 step -> ...은 람다식이고,
         * 의미는 “리스트의 각 step에 대해 이 함수를 적용해라”입니다.
         * 
         * .toList()
         * 변환된 결과들을 다시 List로 모읍니다.
         */

        saveGroupProblemIfNeeded(savedProblem, request.getGroupId(), request.getVisibility());
        saveContestProblemIfNeeded(savedProblem, request.getContestId(), request.getVisibility(), userId);
        revisions.capture(savedProblem);

        return new ProblemCreateObjectiveResponse(
                savedProblem.getId(),
                savedProblem.getType(),
                savedProblem.getTitle(),
                savedProblem.getCreatedAt().toString()
        );
    }

    // 실습형
    @Transactional
    public ProblemCreatePracticeResponse createPractice(long userId, ProblemCreatePracticeRequest request){
        if (!practiceBuildProperties.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "실습 이미지 빌드가 준비되지 않았습니다.");
        }
        List<String> commandPolicy;
        try {
            commandPolicy = PracticeCommandPolicy.normalize(request.getCommandAllowlist());
        } catch (IllegalArgumentException invalid) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "허용 명령어는 영문·숫자로 된 명령어 이름(32자 이하)을 " + PracticeCommandPolicy.MAX_COMMANDS + "개까지 입력할 수 있습니다.");
        }
        var requestedAt = practiceBuildClock.instant();
        User author = userRepository.findById(userId)
                .orElseThrow();

        Problem problem = new Problem(
                author,
                "practice",
                request.getTitle(),
                request.getCategory(),
                request.getDifficulty(),
                request.getVisibility()
        );
        Problem savedProblem = problemRepository.save(problem);

        ProblemPractice problemPractice = new ProblemPractice(
                savedProblem,
                request.getSummary(),
                request.getDescription(),
                hashFlag(request.getFlag().trim()),
                null
        );
        problemPractice.requestBuild(UUID.randomUUID().toString(), requestedAt,
                requestedAt.plus(Duration.ofMinutes(practiceBuildProperties.getDeadlineMinutes())));
        problemPractice.restrictCommands(commandPolicy);
        problemPracticeRepository.save(problemPractice);

        saveGroupProblemIfNeeded(savedProblem, request.getGroupId(), request.getVisibility());
        saveContestProblemIfNeeded(savedProblem, request.getContestId(), request.getVisibility(), userId);

        // Validate links first. Only this request's object can trigger its CodeBuild result.
        s3Service.uploadDockerfileZip(savedProblem.getId(), problemPractice.getBuildRequestId(), request.getDockerfile(),
                commandPolicy);

        return new ProblemCreatePracticeResponse(
                savedProblem.getId(),
                savedProblem.getType(),
                savedProblem.getTitle(),
                savedProblem.getCreatedAt().toString()
        );
    }

    // 코딩형
    @Transactional
    public ProblemCreateCodingResponse createCoding(long userId, ProblemCreateCodingRequest request) {
        if (request == null || request.getConstraints() == null) {
            throw new net.diveon.backend.global.exception.InvalidSubmissionRequestException("코딩 실행 제한이 필요합니다.");
        }
        var limits = request.getConstraints();
        oboValidator.validateQuestion(request.getOboJson(), "coding", null);
        net.diveon.backend.domain.grade.service.CodingExecutionLimits.problem(limits.getTimeLimitMs(),
                limits.getMemoryLimitMb(), limits.getAllowedLanguages(), request.getTestcases());
        oboValidator.validateCodingCases(request.getOboJson(), request.getTestcases());
        User author = userRepository.findById(userId)
                .orElseThrow();

        Problem problem = new Problem(
                author,
                "coding",
                request.getTitle(),
                request.getCategory(),
                request.getDifficulty(),
                request.getVisibility()
        );
        Problem savedProblem = problemRepository.save(problem);

        Boolean oboEnabled = request.getObo() != null ? request.getObo().getEnabled() : false;
        String oboInitialImageUrl = request.getObo() != null ? request.getObo().getInitialImageUrl() : null;

        ProblemCoding problemCoding = new ProblemCoding(
                savedProblem,
                request.getSummary(),
                request.getDescription(),
                request.getInputDescription(),
                request.getOutputDescription(),
                request.getConstraints().getTimeLimitMs(),
                request.getConstraints().getMemoryLimitMb(),
                request.getConstraints().getAllowedLanguages(),
                request.getTestcases(),
                request.getFileUrl(),
                oboEnabled,
                oboInitialImageUrl
        );
        applyOboJson(problemCoding, request.getOboJson());
        problemCodingRepository.save(problemCoding);
        s3Service.uploadCodingTestcases(savedProblem.getId(), request.getTestcases());
        revisions.capture(savedProblem);

        saveGroupProblemIfNeeded(savedProblem, request.getGroupId(), request.getVisibility());
        saveContestProblemIfNeeded(savedProblem, request.getContestId(), request.getVisibility(), userId);

        return new ProblemCreateCodingResponse(
                savedProblem.getId(),
                savedProblem.getType(),
                savedProblem.getTitle(),
                savedProblem.getCreatedAt().toString()
        );
    }

    // 실습형 - flag 원문을 암호화해서 저장 - DB에 정답 노출 안 되도록
    private String hashFlag(String flag) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(flag.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 알고리즘을 찾을 수 없습니다.", e);
        }
    }

    private void saveGroupProblemIfNeeded(Problem problem, Long groupId, String visibility) {
        if ("group".equals(visibility) && groupId != null) {
            Group group = groupRepository.findById(groupId)
                    .orElseThrow(GroupNotFoundException::new);
            groupProblemRepository.save(new GroupProblem(problem, group));
        }
    }

    private void saveContestProblemIfNeeded(Problem problem, Long contestId, String visibility, long userId) {
        if ("contest".equals(visibility) && contestId != null) {
            Contest contest = contestRepository.findById(contestId)
                    .orElseThrow(ContestNotFoundException::new);
            contestParticipantRepository.findByContestIdAndUserId(contestId, userId)
                    .filter(p -> p.getRole() == ContestParticipant.ContestRole.ADMIN)
                    .orElseThrow(ContestAccessDeniedException::new);
            contestProblemRepository.save(new ContestProblem(contest, problem, 100));
        }
    }

    private OboStep toOboStep(Problem problem, ForDtoOboStep step) {
        return new OboStep(problem, step.getStep(), step.getDescription(), step.getImageUrl());
    }

    private void applyOboJson(ProblemObjective problemObjective, JsonNode oboJson) {
        if (oboJson == null) {
            return;
        }
        problemObjective.setOboJsonData(oboJson);
    }

    private void applyOboJson(ProblemCoding problemCoding, JsonNode oboJson) {
        if (oboJson == null) {
            return;
        }
        problemCoding.setOboJsonData(oboJson);
    }
}

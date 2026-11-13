package net.diveon.backend.domain.problem.service;

import net.diveon.backend.domain.problem.dto.request.PracticeRebuildRequest;
import net.diveon.backend.domain.problem.repository.ProblemPracticeRepository;
import net.diveon.backend.domain.problem.repository.ProblemRepository;
import net.diveon.backend.global.exception.ProblemAccessDeniedException;
import net.diveon.backend.global.exception.ProblemNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Rebuilds an existing practice image from a new Dockerfile zip, or from its stored source with a new
 * command policy. The problem is unplayable (PENDING) until the new build is READY; running sessions keep
 * their already-started containers.
 */
@Service
public class PracticeRebuildService {
    private static final Logger log = LoggerFactory.getLogger(PracticeRebuildService.class);
    private final ProblemRepository problems;
    private final ProblemPracticeRepository practices;
    private final S3Service s3;
    private final EcrService ecr;
    private final PracticeBuildProperties properties;
    private final Clock clock;

    public PracticeRebuildService(ProblemRepository problems, ProblemPracticeRepository practices, S3Service s3,
            EcrService ecr, PracticeBuildProperties properties, @Qualifier("practiceBuildClock") Clock clock) {
        this.problems = problems; this.practices = practices; this.s3 = s3; this.ecr = ecr;
        this.properties = properties; this.clock = clock;
    }

    public record Result(long probId, String status) {}

    @Transactional
    public Result rebuild(long userId, long probId, PracticeRebuildRequest request) {
        if (!properties.isEnabled())
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "실습 이미지 빌드가 준비되지 않았습니다.");
        List<String> policy;
        try {
            policy = PracticeCommandPolicy.normalize(request.getCommandAllowlist());
        } catch (IllegalArgumentException invalid) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "허용 명령어는 영문·숫자로 된 명령어 이름(32자 이하)을 " + PracticeCommandPolicy.MAX_COMMANDS + "개까지 입력할 수 있습니다.");
        }
        var problem = problems.findById(probId).orElseThrow(ProblemNotFoundException::new);
        if (!problem.getAuthor().getId().equals(userId)) throw new ProblemAccessDeniedException();
        var practice = practices.findBuildForUpdate(probId).orElseThrow(ProblemNotFoundException::new);
        if ("PENDING".equals(practice.getImageStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "실습 환경을 만드는 중에는 다시 만들 수 없습니다.");

        String oldRequest = practice.getBuildRequestId();
        String oldBuild = practice.getCodeBuildId();
        List<String> oldPolicy = practice.getCommandAllowlist();
        boolean oldImage = "READY".equals(practice.getImageStatus());
        var file = request.getDockerfile();
        boolean newSource = file != null && !file.isEmpty();
        // Legacy builds have no request-scoped source to copy; decide before changing any state.
        if (!newSource && (oldRequest == null || !s3.hasDockerfileZip(probId, oldRequest)))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이전 Dockerfile을 찾을 수 없습니다. Dockerfile을 다시 올려 주세요.");

        var now = clock.instant();
        String requestId = UUID.randomUUID().toString();
        practice.requestBuild(requestId, now, now.plus(Duration.ofMinutes(properties.getDeadlineMinutes())));
        practice.restrictCommands(policy);
        practices.save(practice);
        if (newSource) s3.uploadDockerfileZip(probId, requestId, file, policy);
        else s3.copyDockerfileZip(probId, oldRequest, requestId, policy);

        afterCommit(() -> cleanup(probId, oldRequest, oldImage ? oldBuild : null, oldImage, oldPolicy));
        return new Result(probId, practice.getImageStatus());
    }

    /** Old artifacts are removed only once the new request is committed; failures leave them for later. */
    private void cleanup(long probId, String oldRequest, String oldBuild, boolean oldImage, List<String> oldPolicy) {
        if (oldRequest != null) {
            try { s3.deleteDockerfileZip(probId, oldRequest); }
            catch (RuntimeException e) { log.warn("이전 실습 소스 삭제 실패 - probId: {}", probId, e); }
        }
        if (oldImage) {
            try { ecr.deleteImage(probId, oldBuild, oldPolicy); }
            catch (RuntimeException e) { log.warn("이전 실습 이미지 삭제 실패 - probId: {}", probId, e); }
        }
    }

    private static void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) { action.run(); return; }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { action.run(); }
        });
    }
}

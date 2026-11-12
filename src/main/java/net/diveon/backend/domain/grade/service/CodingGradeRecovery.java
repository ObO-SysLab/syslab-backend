package net.diveon.backend.domain.grade.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.diveon.backend.domain.grade.dto.message.CodingGradeMessage;
import net.diveon.backend.domain.grade.repository.SolveSubmissionCodingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Profile("prod")
@ConditionalOnProperty(name = "grading.recovery.enabled", havingValue = "true")
public class CodingGradeRecovery {
    private static final Logger log = LoggerFactory.getLogger(CodingGradeRecovery.class);
    private final CodingGradeJobStore jobs;
    private final CodingGradeQueueServiceImpl queue;
    private final SolveSubmissionCodingRepository submissions;
    private final ObjectMapper mapper;
    private final int maxAttempts;
    private final TaskExecutor grading;

    public CodingGradeRecovery(CodingGradeJobStore jobs, CodingGradeQueueServiceImpl queue,
            SolveSubmissionCodingRepository submissions, ObjectMapper mapper,
            @Value("${grading.recovery.max-attempts:3}") int maxAttempts,
            @Qualifier("gradingScheduler") TaskExecutor grading) {
        if (maxAttempts < 1 || maxAttempts > 3) throw new IllegalArgumentException("max-attempts must be 1..3");
        this.jobs = jobs;
        this.queue = queue;
        this.submissions = submissions;
        this.mapper = mapper;
        this.maxAttempts = maxAttempts;
        this.grading = grading;
    }

    // 제출이 커밋되면 15초 주기를 기다리지 않고 바로 보낸다. 같은 채점 전용 스레드에 넣으므로 주기 작업과 겹치지 않는다.
    // 넣지 못해도(스레드 종료 등) 작업표에 남아 있어 다음 주기 작업이 보낸다.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void dispatchAfterCommit(CodingGradeRequested requested) {
        try {
            grading.execute(this::dispatchDueJobs);
        } catch (RuntimeException e) {
            log.warn("Immediate grading dispatch was not queued: submission={}, type={}",
                    requested.submissionId(), e.getClass().getSimpleName());
        }
    }

    // 알림·티어·랭킹 작업과 한 스레드를 나눠 쓰지 않도록 채점 전용 스케줄러에서 돈다(GradingConfig).
    @Scheduled(fixedDelayString = "${grading.recovery.poll-ms:15000}", scheduler = "gradingScheduler")
    public void dispatchDueJobs() {
        // Claim commits before external I/O. A process crash is recovered at the deadline.
        for (int i = 0; i < 20; i++) {
            var job = jobs.claimNext(maxAttempts);
            if (job == null) return;
            if (job.payload() == null) continue;
            try {
                var message = mapper.readValue(job.payload(), CodingGradeMessage.class);
                message.setAttemptNo(job.attempt());
                var code = submissions.findById(job.submissionId()).orElseThrow();
                queue.dispatch(message, code.getAnswer());
            } catch (Exception e) {
                jobs.dispatchFailed(job);
                // Never log payloads, source code or credentials from SDK exception messages.
                log.warn("Grading dispatch failed: submission={}, attempt={}, type={}",
                        job.submissionId(), job.attempt(), e.getClass().getSimpleName());
                // Retry on the next scheduled tick, rather than exhausting all attempts immediately.
                return;
            }
        }
    }
}

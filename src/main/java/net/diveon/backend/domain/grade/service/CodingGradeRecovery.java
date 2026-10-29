package net.diveon.backend.domain.grade.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.diveon.backend.domain.grade.dto.message.CodingGradeMessage;
import net.diveon.backend.domain.grade.repository.SolveSubmissionCodingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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

    public CodingGradeRecovery(CodingGradeJobStore jobs, CodingGradeQueueServiceImpl queue,
            SolveSubmissionCodingRepository submissions, ObjectMapper mapper,
            @Value("${grading.recovery.max-attempts:3}") int maxAttempts) {
        if (maxAttempts < 1 || maxAttempts > 3) throw new IllegalArgumentException("max-attempts must be 1..3");
        this.jobs = jobs;
        this.queue = queue;
        this.submissions = submissions;
        this.mapper = mapper;
        this.maxAttempts = maxAttempts;
    }

    @Scheduled(fixedDelayString = "${grading.recovery.poll-ms:15000}")
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

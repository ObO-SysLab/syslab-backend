package net.diveon.backend.domain.problem.service;

import net.diveon.backend.domain.problem.repository.ProblemPracticeRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;

@Service
public class PracticeBuildStore {
    private final ProblemPracticeRepository repository;
    private final Clock clock;

    public PracticeBuildStore(ProblemPracticeRepository repository, @Qualifier("practiceBuildClock") Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    // AWS calls must finish before entering this short transaction.
    @Transactional
    public void apply(PracticeBuildIdentity identity, String buildId, String status, String imageUri) {
        apply(identity, buildId, status, imageUri, null);
    }

    @Transactional
    public void apply(PracticeBuildIdentity identity, String buildId, String status, String imageUri, String failureDetail) {
        var practice = repository.findBuildForUpdate(identity.problemId())
                .orElseThrow(() -> new IllegalStateException("Build request not committed or removed"));
        practice.applyBuild(identity.requestId(), buildId, status, imageUri, clock.instant(), failureDetail);
    }
}

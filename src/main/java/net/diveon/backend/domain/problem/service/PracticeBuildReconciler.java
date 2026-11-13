package net.diveon.backend.domain.problem.service;

import net.diveon.backend.domain.problem.entity.ProblemPractice;
import net.diveon.backend.domain.problem.repository.ProblemPracticeRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Clock;
import java.util.*;

@Service
@Profile("prod")
public class PracticeBuildReconciler {
    private static final Logger log = LoggerFactory.getLogger(PracticeBuildReconciler.class);
    private final PracticeBuildGateway gateway;
    private final PracticeBuildStore store;
    private final ProblemPracticeRepository repository;
    private final PracticeBuildProperties properties;
    private final Clock clock;
    private long afterId;

    public PracticeBuildReconciler(PracticeBuildGateway gateway, PracticeBuildStore store,
            ProblemPracticeRepository repository, PracticeBuildProperties properties,
            @Qualifier("practiceBuildClock") Clock clock) {
        this.gateway = gateway;
        this.store = store;
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
    }

    public void accept(String buildId) { apply(gateway.get(buildId)); }

    private boolean matches(ProblemPractice p, PracticeBuildGateway.Build build) {
        return "PENDING".equals(p.getImageStatus()) && build.identity().requestId().equals(p.getBuildRequestId())
                && (p.getCodeBuildId() == null || p.getCodeBuildId().equals(build.id()));
    }

    private void apply(PracticeBuildGateway.Build build) {
        var practice = repository.findById(build.identity().problemId())
                .orElseThrow(() -> new IllegalStateException("Build request not committed or removed"));
        if (!matches(practice, build)) return;
        if ("SUCCEEDED".equals(build.status())) {
            // Bind before ECR lookup; a concurrent duplicate cannot select another artifact.
            store.apply(build.identity(), build.id(), "IN_PROGRESS", null);
            var bound = repository.findById(build.identity().problemId()).orElseThrow();
            if (!matches(bound, build) || !build.id().equals(bound.getCodeBuildId())) return;
            var policy = bound.getCommandAllowlist();
            String image;
            try {
                image = gateway.imageUri(build, policy == null ? null : PracticeCommandPolicy.markerTag(build.id(), policy));
            } catch (PracticeBuildGateway.RestrictionUnverifiedException unrestricted) {
                // Never offer an image that lacks the requested restriction (e.g. an old buildspec).
                store.apply(build.identity(), build.id(), ProblemPractice.RESTRICTION_UNVERIFIED, null);
                return;
            }
            store.apply(build.identity(), build.id(), build.status(), image);
        } else if ("IN_PROGRESS".equals(build.status())) {
            store.apply(build.identity(), build.id(), build.status(), null);
        } else {
            // Read the author-facing reason before the short row-locking transaction.
            store.apply(build.identity(), build.id(), build.status(), null, failureDetail(build.identity()));
        }
    }

    private String failureDetail(PracticeBuildIdentity identity) {
        try { return gateway.failureDetail(identity); }
        catch (RuntimeException unreadable) { return null; } // Never delay the terminal state.
    }

    @Scheduled(fixedDelayString = "${practice.build.poll-ms:30000}", scheduler = "practiceBuildScheduler")
    public void expire() {
        if (properties.isEnabled() && properties.isRecoveryEnabled()) repository.expireBuilds(clock.instant());
    }

    @Scheduled(fixedDelayString = "${practice.build.poll-ms:30000}", scheduler = "practiceBuildScheduler")
    public void recover() {
        if (!properties.isEnabled() || !properties.isRecoveryEnabled()) return;
        // Expiration does not depend on AWS availability or discovery pagination.
        repository.expireBuilds(clock.instant());
        var pending = repository.findPendingBuilds(afterId, PageRequest.of(0, properties.getBatchSize()));
        afterId = pending.isEmpty() ? 0 : pending.getLast().getProbId();
        Set<PracticeBuildIdentity> unknown = new HashSet<>();
        for (var practice : pending) {
            if (practice.getCodeBuildId() == null) {
                unknown.add(new PracticeBuildIdentity(practice.getProbId(), practice.getBuildRequestId()));
            } else {
                try { accept(practice.getCodeBuildId()); }
                catch (RuntimeException e) { log.warn("Practice build lookup deferred: problem={}, error={}", practice.getProbId(), e.getClass().getSimpleName()); }
            }
        }
        try {
            for (var build : gateway.discover(unknown).values()) {
                try { apply(build); }
                catch (RuntimeException e) { log.warn("Practice build recovery deferred: problem={}, error={}", build.identity().problemId(), e.getClass().getSimpleName()); }
            }
        } catch (RuntimeException e) {
            log.warn("Practice build discovery deferred: error={}", e.getClass().getSimpleName());
        }
        repository.expireBuilds(clock.instant());
    }
}

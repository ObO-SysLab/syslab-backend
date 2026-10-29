package net.diveon.backend.domain.problem.service;

import net.diveon.backend.domain.problem.entity.ProblemPractice;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.time.Instant;
import static org.assertj.core.api.Assertions.*;

class PracticeBuildStateTest {
    static final String REQUEST = "00000000-0000-4000-8000-000000000001";
    static final String BUILD = "syslab-practice-build:00000000-0000-4000-8000-000000000002";
    static final String IMAGE = "registry/repo@sha256:" + "a".repeat(64);
    static final Instant START = Instant.parse("2026-10-30T00:00:00Z");
    static ProblemPractice pending() {
        var p = new ProblemPractice();
        p.requestBuild(REQUEST, START, START.plusSeconds(600));
        return p;
    }

    @ParameterizedTest @ValueSource(strings = {"FAILED", "FAULT", "STOPPED", "TIMED_OUT"})
    void everyFailureIsTerminal(String status) {
        var p = pending();
        assertThat(p.applyBuild(REQUEST, BUILD, status, null, START)).isTrue();
        assertThat(p.getImageStatus()).isEqualTo("FAILED");
        assertThat(p.getBuildFailureReason()).isEqualTo(status);
        assertThat(p.applyBuild(REQUEST, BUILD, "SUCCEEDED", IMAGE, START.plusSeconds(1))).isFalse();
        assertThat(p.getEcrImageUri()).isNull();
    }

    @Test void successKeepsVerifiedDigestAndIgnoresDuplicateFailure() {
        var p = pending();
        p.applyBuild(REQUEST, BUILD, "SUCCEEDED", IMAGE, START);
        p.applyBuild(REQUEST, BUILD, "FAILED", null, START.plusSeconds(1));
        assertThat(p.getImageStatus()).isEqualTo("READY");
        assertThat(p.getEcrImageUri()).isEqualTo(IMAGE);
    }

    @Test void deadlineIsInclusiveAndLateSuccessCannotRevive() {
        var p = pending();
        p.applyBuild(REQUEST, BUILD, "SUCCEEDED", IMAGE, START.plusSeconds(600));
        assertThat(p.getImageStatus()).isEqualTo("FAILED");
        assertThat(p.getBuildFailureReason()).isEqualTo("DEADLINE_EXCEEDED");
        p.applyBuild(REQUEST, BUILD, "SUCCEEDED", IMAGE, START.plusSeconds(601));
        assertThat(p.getEcrImageUri()).isNull();
    }

    @Test void successBeforeDeadlineIsAllowed() {
        var p = pending();
        p.applyBuild(REQUEST, BUILD, "SUCCEEDED", IMAGE, START.plusSeconds(599));
        assertThat(p.getImageStatus()).isEqualTo("READY");
    }

    @Test void oldRequestAndSecondBuildCannotOverwriteCurrentBuild() {
        var p = pending();
        assertThat(p.applyBuild(REQUEST.replace("001", "003"), BUILD, "FAILED", null, START)).isFalse();
        p.applyBuild(REQUEST, BUILD, "IN_PROGRESS", null, START);
        assertThat(p.applyBuild(REQUEST, BUILD.replace("002", "003"), "FAILED", null, START)).isFalse();
        assertThat(p.getImageStatus()).isEqualTo("PENDING");
        assertThat(p.getCodeBuildId()).isEqualTo(BUILD);
    }

    @Test void legacyPendingCannotAdoptAnUnrelatedBuild() {
        var p = new ProblemPractice();
        assertThat(p.applyBuild(REQUEST, BUILD, "SUCCEEDED", IMAGE, START)).isFalse();
    }

    @Test void unknownStatusAndMutableTagAreRejected() {
        var p = pending();
        assertThatThrownBy(() -> p.applyBuild(REQUEST, BUILD, "NEW_STATE", null, START)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> p.applyBuild(REQUEST, BUILD, "SUCCEEDED", "repo:prob-1", START)).isInstanceOf(IllegalArgumentException.class);
        assertThat(p.getImageStatus()).isEqualTo("PENDING");
        assertThat(p.getCodeBuildId()).isNull();
    }

    @ParameterizedTest @ValueSource(strings = {"prob-1/source.zip", "x/prob-1/builds/00000000-0000-4000-8000-000000000001/source.zip", "prob-1/../source.zip"})
    void sourceIdentityMustBeExact(String key) {
        assertThatThrownBy(() -> PracticeBuildIdentity.fromKey(key)).isInstanceOf(IllegalArgumentException.class);
    }
}

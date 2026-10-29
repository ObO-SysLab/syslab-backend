package net.diveon.backend.domain.problem.service;

import net.diveon.backend.domain.problem.repository.ProblemPracticeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.*;
import static net.diveon.backend.domain.problem.service.PracticeBuildStateTest.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PracticeBuildReconcilerTest {
    final ProblemPracticeRepository repo = mock(ProblemPracticeRepository.class);
    final PracticeBuildGateway gateway = mock(PracticeBuildGateway.class);
    final PracticeBuildProperties props = new PracticeBuildProperties();
    final Clock clock = Clock.fixed(START, ZoneOffset.UTC);
    final PracticeBuildStore store = new PracticeBuildStore(repo, clock);
    final PracticeBuildReconciler reconciler = new PracticeBuildReconciler(gateway, store, repo, props, clock);
    final PracticeBuildIdentity identity = new PracticeBuildIdentity(1, REQUEST);
    final net.diveon.backend.domain.problem.entity.ProblemPractice practice = pending();

    @BeforeEach void setup() {
        ReflectionTestUtils.setField(practice, "probId", 1L);
        when(repo.findById(1L)).thenReturn(Optional.of(practice));
        when(repo.findBuildForUpdate(1L)).thenReturn(Optional.of(practice));
        when(repo.findPendingBuilds(anyLong(), any())).thenReturn(List.of(practice));
        props.setEnabled(true);
        props.setRecoveryEnabled(true);
    }

    @Test void missingEventRecoversFromDiscoveryAndBindsDigest() {
        var build = new PracticeBuildGateway.Build(BUILD, identity, "SUCCEEDED");
        when(gateway.discover(Set.of(identity))).thenReturn(Map.of(identity, build));
        when(gateway.imageUri(build)).thenReturn(IMAGE);
        reconciler.recover();
        assertThat(practice.getImageStatus()).isEqualTo("READY");
        assertThat(practice.getCodeBuildId()).isEqualTo(BUILD);
        assertThat(practice.getEcrImageUri()).isEqualTo(IMAGE);
        var order = inOrder(repo, gateway);
        order.verify(repo).expireBuilds(START);
        order.verify(repo).findPendingBuilds(anyLong(), any());
        order.verify(gateway).discover(Set.of(identity));
    }

    @Test void awsOutageStillRunsDeadlineExpiration() {
        when(gateway.discover(anySet())).thenThrow(new IllegalStateException("offline"));
        reconciler.recover();
        verify(repo, times(2)).expireBuilds(START);
        assertThat(practice.getImageStatus()).isEqualTo("PENDING");
    }

    @Test void knownBuildIsRecheckedWithoutEvent() {
        practice.applyBuild(REQUEST, BUILD, "IN_PROGRESS", null, START);
        when(gateway.get(BUILD)).thenReturn(new PracticeBuildGateway.Build(BUILD, identity, "STOPPED"));
        reconciler.recover();
        assertThat(practice.getImageStatus()).isEqualTo("FAILED");
        verify(gateway).discover(Set.of());
    }

    @Test void missingDigestRetriesWithoutDeclaringSuccess() {
        var build = new PracticeBuildGateway.Build(BUILD, identity, "SUCCEEDED");
        when(gateway.get(BUILD)).thenReturn(build);
        when(gateway.imageUri(build)).thenThrow(new IllegalStateException("not visible"));
        assertThatThrownBy(() -> reconciler.accept(BUILD)).isInstanceOf(IllegalStateException.class);
        assertThat(practice.getImageStatus()).isEqualTo("PENDING");
        assertThat(practice.getCodeBuildId()).isEqualTo(BUILD);
        doReturn(IMAGE).when(gateway).imageUri(build);
        reconciler.accept(BUILD);
        assertThat(practice.getImageStatus()).isEqualTo("READY");
    }

    @Test void oldRequestDoesNotFetchImageOrWrite() {
        var old = new PracticeBuildIdentity(1, REQUEST.replace("001", "003"));
        when(gateway.get(BUILD)).thenReturn(new PracticeBuildGateway.Build(BUILD, old, "SUCCEEDED"));
        reconciler.accept(BUILD);
        verify(repo, never()).findBuildForUpdate(anyLong());
        verify(gateway, never()).imageUri(any());
    }

    @Test void disabledRecoveryHasNoExternalCalls() {
        props.setRecoveryEnabled(false);
        reconciler.recover();
        verifyNoInteractions(gateway);
        verify(repo, never()).expireBuilds(any());
    }

    @Test void independentExpirationNeverWaitsForAws() {
        reconciler.expire();
        verify(repo).expireBuilds(START);
        verifyNoInteractions(gateway);
    }

    @Test void storeRechecksTimeAfterSlowExternalLookup() {
        var lateStore = new PracticeBuildStore(repo, Clock.fixed(START.plusSeconds(600), ZoneOffset.UTC));
        lateStore.apply(identity, BUILD, "SUCCEEDED", IMAGE);
        assertThat(practice.getImageStatus()).isEqualTo("FAILED");
        assertThat(practice.getBuildFailureReason()).isEqualTo("DEADLINE_EXCEEDED");
        assertThat(practice.getEcrImageUri()).isNull();
    }

    @Test void pageCursorMovesPastStuckRowsAndWraps() {
        when(repo.findPendingBuilds(eq(1L), any())).thenReturn(List.of());
        reconciler.recover();
        reconciler.recover();
        reconciler.recover();
        verify(repo, times(2)).findPendingBuilds(eq(0L), any());
        verify(repo).findPendingBuilds(eq(1L), any());
    }
}

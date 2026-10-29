package net.diveon.backend.domain.vm.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.*;
import net.diveon.backend.domain.problem.entity.ProblemPractice;
import net.diveon.backend.domain.problem.repository.ProblemPracticeRepository;
import net.diveon.backend.domain.problem.service.ProblemAccessService;
import net.diveon.backend.domain.vm.dto.request.*;
import net.diveon.backend.domain.vm.entity.VmSession;
import net.diveon.backend.domain.vm.repository.VmSessionRepository;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

class VmLifecycleTest {
    VmSessionRepository sessions = mock(VmSessionRepository.class);
    ProblemPracticeRepository practices = mock(ProblemPracticeRepository.class);
    DockerService docker = mock(DockerService.class);
    ProblemAccessService access = mock(ProblemAccessService.class);
    VmSessionStore store = mock(VmSessionStore.class);
    Clock clock = Clock.fixed(Instant.parse("2026-10-30T00:00:00Z"), ZoneOffset.UTC);
    LocalDateTime now = LocalDateTime.now(clock);
    String cid = "a".repeat(64);
    VmService service = new VmService(sessions, practices, docker, access, store, clock);
    VmSession vm;

    @BeforeEach void setup() {
        var practice = mock(ProblemPractice.class);
        when(practices.findById(9L)).thenReturn(Optional.of(practice));
        when(practice.getImageStatus()).thenReturn("READY");
        when(practice.getEcrImageUri()).thenReturn("registry/practice:latest");
        vm = VmSession.creating(7L, 9L, now);
        ReflectionTestUtils.setField(vm, "id", 10L);
        when(store.exact(7L, 9L, 10L)).thenReturn(vm);
        when(store.stopping(7L, 9L, 10L)).thenAnswer(i -> { vm.stopping(); return vm; });
    }
    VmCreateRequest create() { var r = new VmCreateRequest(); r.setProbId(9L); return r; }
    VmStopRequest stop(long id) {
        var r = new VmStopRequest(); ReflectionTestUtils.setField(r, "probId", 9L);
        ReflectionTestUtils.setField(r, "sessionId", id); return r;
    }
    void reused() {
        vm.running(cid);
        when(store.reserve(eq(7L), eq(9L), any())).thenReturn(new VmSessionStore.Reservation(vm, false));
        when(docker.inspect(any())).thenReturn(new DockerService.State("RUNNING", cid));
    }
    @Test void creationCommitsIntentBeforeRemoteAndActivation() {
        when(store.reserve(eq(7L), eq(9L), any())).thenReturn(new VmSessionStore.Reservation(vm, true));
        when(docker.runContainer(anyString(), any())).thenReturn(cid);
        when(store.activate(7L, 9L, 10L, cid, now)).thenAnswer(i -> { vm.running(cid); return vm; });
        assertEquals(10L, service.createVm(7L, create()).getSessionId());
        var ordered = inOrder(store, docker);
        ordered.verify(store).reserve(7L, 9L, now);
        ordered.verify(docker).runContainer(eq("registry/practice:latest"), argThat(t -> t.sessionId() == 10 && t.legacyContainerId() == null));
        ordered.verify(store).activate(7L, 9L, 10L, cid, now);
    }
    @Test void healthyReuseDoesNotExtendExpiryOrRunAgain() {
        reused(); var expiry = vm.getExpiresAt();
        assertEquals(expiry, service.createVm(7L, create()).getExpiresAt());
        verify(docker, never()).runContainer(any(), any()); verify(store, never()).activate(any(), any(), any(), any(), any());
    }
    @Test void remoteOutageDoesNotCauseReplacement() {
        reused(); when(docker.inspect(any())).thenThrow(new IllegalStateException());
        assertEquals(503, assertThrows(ResponseStatusException.class, () -> service.createVm(7L, create())).getStatusCode().value());
        verify(docker, never()).runContainer(any(), any()); verify(docker, never()).stopAndRemoveContainer(any());
    }
    @Test void expiredSessionIsCleanedBeforeNewReservation() {
        reused(); ReflectionTestUtils.setField(vm, "expiresAt", now);
        when(store.reserve(7L, 9L, now)).thenReturn(new VmSessionStore.Reservation(vm, false)).thenThrow(VmSessionStore.busy());
        assertThrows(ResponseStatusException.class, () -> service.createVm(7L, create()));
        var ordered = inOrder(store, docker);
        ordered.verify(store).reserve(7L, 9L, now); ordered.verify(store).stopping(7L, 9L, 10L);
        ordered.verify(docker).stopAndRemoveContainer(any()); ordered.verify(store).stopped(7L, 9L, 10L);
        ordered.verify(store).reserve(7L, 9L, now);
        verify(docker, never()).inspect(any());
    }
    @Test void failedCleanupNeverMarksStoppedOrCreatesNewVm() {
        reused(); ReflectionTestUtils.setField(vm, "expiresAt", now);
        doThrow(new IllegalStateException()).when(docker).stopAndRemoveContainer(any());
        assertThrows(ResponseStatusException.class, () -> service.createVm(7L, create()));
        assertEquals("STOPPING", vm.getStatus());
        verify(store, never()).stopped(any(), any(), any()); verify(docker, never()).runContainer(any(), any());
        verify(store, times(1)).reserve(any(), any(), any());
    }
    @Test void lostCreateResponseStillCleansUsingReservedIdentity() {
        when(store.reserve(7L, 9L, now)).thenReturn(new VmSessionStore.Reservation(vm, true));
        when(docker.runContainer(any(), any())).thenThrow(new IllegalStateException());
        assertThrows(ResponseStatusException.class, () -> service.createVm(7L, create()));
        verify(docker).stopAndRemoveContainer(argThat(t -> t.sessionId() == 10 && t.legacyContainerId() == null));
        verify(store).stopped(7L, 9L, 10L);
    }
    @Test void activationFailureCompensatesCreatedContainer() {
        when(store.reserve(7L, 9L, now)).thenReturn(new VmSessionStore.Reservation(vm, true));
        when(docker.runContainer(any(), any())).thenReturn(cid);
        when(store.activate(any(), any(), any(), any(), any())).thenThrow(VmSessionStore.busy());
        assertThrows(ResponseStatusException.class, () -> service.createVm(7L, create()));
        verify(docker).stopAndRemoveContainer(any()); verify(store).stopped(7L, 9L, 10L);
    }
    @Test void oldStoppedSessionDoesNotTouchNewSession() {
        vm.stop(); doReturn(vm).when(store).stopping(7L, 9L, 10L);
        service.stopVm(7L, stop(10));
        verifyNoInteractions(docker); verifyNoInteractions(sessions);
    }
    @Test void forgedSessionCannotReachRemote() {
        when(store.exact(7L, 9L, 11L)).thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
        assertThrows(ResponseStatusException.class, () -> service.stopVm(7L, stop(11)));
        verifyNoInteractions(docker);
    }
    @Test void resetCannotDeleteNewSessionInsertedAfterCleanup() {
        vm.running(cid); when(store.beginReset(7L, 9L, 10L)).thenAnswer(i -> { vm.stopping(); return vm; });
        var newer = VmSession.creating(7L, 9L, now); ReflectionTestUtils.setField(newer, "id", 11L); newer.running("b".repeat(64));
        when(store.reserve(7L, 9L, now)).thenReturn(new VmSessionStore.Reservation(newer, false));
        assertThrows(ResponseStatusException.class, () -> service.resetVm(7L, stop(10)));
        verify(docker, times(1)).stopAndRemoveContainer(argThat(t -> t.sessionId() == 10));
        verify(docker, never()).runContainer(any(), any()); verify(store, never()).stopping(7L, 9L, 11L);
    }
    @Test void statusOutageNeverReportsMissingOrStops() {
        vm.running(cid); when(sessions.findAllByUserIdAndStatusIn(7L, VmSessionStore.ACTIVE)).thenReturn(List.of(vm));
        when(docker.inspect(any())).thenThrow(new IllegalStateException());
        assertThrows(ResponseStatusException.class, () -> service.getStatus(7L, 9L));
        verify(store, never()).stopping(any(), any(), any());
    }
    @Test void statusShowsCleanupPendingWithoutCallingDocker() {
        vm.stopping(); when(sessions.findAllByUserIdAndStatusIn(7L, VmSessionStore.ACTIVE)).thenReturn(List.of(vm));
        assertEquals("STOPPING", service.getStatus(7L, 9L).getStatus()); verifyNoInteractions(docker);
    }
    @Test void recoveryIgnoresFreshReservationButCleansStaleCreation() {
        when(sessions.findById(10L)).thenReturn(Optional.of(vm));
        service.reconcile(10L); verifyNoInteractions(docker);
        ReflectionTestUtils.setField(vm, "createdAt", now.minusMinutes(2));
        service.reconcile(10L); verify(docker).stopAndRemoveContainer(any());
    }
    @Test void recoveryRetriesStoppingWithoutAccessRecheck() {
        vm.stopping(); when(sessions.findById(10L)).thenReturn(Optional.of(vm));
        service.reconcile(10L); verifyNoInteractions(access); verify(store).stopped(7L, 9L, 10L);
    }
    @Test void recoveryCleansExpiredVmWithoutDockerInspect() {
        vm.running(cid); ReflectionTestUtils.setField(vm, "expiresAt", now);
        when(sessions.findById(10L)).thenReturn(Optional.of(vm));
        service.reconcile(10L); verify(docker, never()).inspect(any()); verify(docker).stopAndRemoveContainer(any());
    }
    @Test void legacyMissingIdCannotBeMistakenForManagedMissingContainer() {
        ReflectionTestUtils.setField(vm, "runtimeVersion", 0);
        assertThrows(ResponseStatusException.class, () -> service.stopVm(7L, stop(10)));
        assertEquals("STOPPING", vm.getStatus()); verifyNoInteractions(docker);
        verify(store, never()).stopped(any(), any(), any());
    }
    @Test void unexpectedContainerIdentityDoesNotTriggerDeleteOrReuse() {
        reused(); when(docker.inspect(any())).thenReturn(new DockerService.State("RUNNING", "b".repeat(64)));
        assertThrows(ResponseStatusException.class, () -> service.createVm(7L, create()));
        verify(docker, never()).stopAndRemoveContainer(any()); verify(docker, never()).runContainer(any(), any());
    }
}

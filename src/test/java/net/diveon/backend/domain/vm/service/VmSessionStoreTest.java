package net.diveon.backend.domain.vm.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.LocalDateTime;
import java.util.*;
import net.diveon.backend.domain.user.entity.User;
import net.diveon.backend.domain.user.repository.UserRepository;
import net.diveon.backend.domain.vm.entity.VmSession;
import net.diveon.backend.domain.vm.repository.VmSessionRepository;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

class VmSessionStoreTest {
    UserRepository users = mock(UserRepository.class);
    VmSessionRepository sessions = mock(VmSessionRepository.class);
    VmSessionStore store = new VmSessionStore(users, sessions);
    LocalDateTime now = LocalDateTime.of(2026, 10, 30, 0, 0);
    VmSession vm;
    @BeforeEach void setup() {
        vm = VmSession.creating(7L, 9L, now); ReflectionTestUtils.setField(vm, "id", 10L);
        when(users.lockVmOwner(7L)).thenReturn(Optional.of(new User()));
        when(sessions.findByIdAndUserIdAndProbId(10L, 7L, 9L)).thenReturn(Optional.of(vm));
        when(sessions.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
    }
    @Test void reserveLocksOwnerBeforeCheckingAndPersistingIntent() {
        when(sessions.findAllByUserIdAndStatusIn(7L, VmSessionStore.ACTIVE)).thenReturn(List.of());
        var r = store.reserve(7L, 9L, now);
        assertTrue(r.created()); assertEquals("CREATING", r.session().getStatus());
        assertEquals(now.plusHours(2), r.session().getExpiresAt()); assertEquals(1, r.session().getRuntimeVersion());
        var ordered = inOrder(users, sessions);
        ordered.verify(users).lockVmOwner(7L); ordered.verify(sessions).findAllByUserIdAndStatusIn(7L, VmSessionStore.ACTIVE);
        ordered.verify(sessions).saveAndFlush(any());
    }
    @Test void creatingAndStoppingBlockAdditionalReservations() {
        when(sessions.findAllByUserIdAndStatusIn(7L, VmSessionStore.ACTIVE)).thenReturn(List.of(vm));
        assertThrows(ResponseStatusException.class, () -> store.reserve(7L, 9L, now));
        vm.stopping(); assertThrows(ResponseStatusException.class, () -> store.reserve(7L, 9L, now));
        verify(sessions, never()).saveAndFlush(any());
    }
    @Test void legacyDuplicatesFailClosed() {
        vm.running("a".repeat(64));
        when(sessions.findAllByUserIdAndStatusIn(7L, VmSessionStore.ACTIVE)).thenReturn(List.of(vm, vm));
        assertThrows(ResponseStatusException.class, () -> store.reserve(7L, 9L, now));
    }
    @Test void stoppedOrStoppingCannotBeResurrectedByLateActivation() {
        for (String status : List.of("STOPPING", "STOPPED")) {
            ReflectionTestUtils.setField(vm, "status", status);
            assertThrows(ResponseStatusException.class, () -> store.activate(7L, 9L, 10L, "a".repeat(64), now));
        }
    }
    @Test void expiredCreationCannotActivate() {
        assertThrows(ResponseStatusException.class, () -> store.activate(7L, 9L, 10L, "a".repeat(64), now.plusHours(2)));
    }
    @Test void stoppingIsCommittedAndIdempotent() {
        assertEquals("STOPPING", store.stopping(7L, 9L, 10L).getStatus());
        assertEquals("STOPPING", store.stopping(7L, 9L, 10L).getStatus());
        verify(sessions, times(2)).saveAndFlush(vm);
    }
    @Test void onlyCleanupIntentCanBecomeStopped() {
        assertThrows(ResponseStatusException.class, () -> store.stopped(7L, 9L, 10L));
        vm.stopping(); store.stopped(7L, 9L, 10L); assertEquals("STOPPED", vm.getStatus());
    }
    @Test void resetRejectsOldStoppedSession() {
        vm.stop(); assertThrows(ResponseStatusException.class, () -> store.beginReset(7L, 9L, 10L));
        verify(sessions, never()).saveAndFlush(any());
    }
    @Test void exactLookupScopesOwnerAndProblemAndRequiresId() {
        assertThrows(ResponseStatusException.class, () -> store.exact(8L, 9L, 10L));
        assertThrows(ResponseStatusException.class, () -> store.exact(7L, 8L, 10L));
        assertThrows(ResponseStatusException.class, () -> store.exact(7L, 9L, null));
        assertThrows(ResponseStatusException.class, () -> store.exact(7L, 9L, 0L));
    }
}

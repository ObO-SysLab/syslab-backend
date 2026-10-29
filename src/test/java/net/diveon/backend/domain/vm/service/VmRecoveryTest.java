package net.diveon.backend.domain.vm.service;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDateTime;
import java.util.List;
import net.diveon.backend.domain.vm.entity.VmSession;
import net.diveon.backend.domain.vm.repository.VmSessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class VmRecoveryTest {
    @Test void recoveryRequiresExplicitProductionOptIn() {
        for (boolean enabled : new boolean[]{false, true}) {
            var context = new org.springframework.context.annotation.AnnotationConfigApplicationContext();
            context.getEnvironment().setActiveProfiles("prod");
            if (enabled) context.getEnvironment().getPropertySources().addFirst(
                new org.springframework.core.env.MapPropertySource("test", java.util.Map.of("vm.recovery.enabled", "true")));
            context.registerBean(VmSessionRepository.class, () -> mock(VmSessionRepository.class));
            context.registerBean(VmService.class, () -> mock(VmService.class));
            context.registerBean(DockerService.class, () -> mock(DockerService.class));
            context.register(VmRecovery.class);
            try { context.refresh(); assertEquals(enabled ? 1 : 0, context.getBeansOfType(VmRecovery.class).size()); }
            finally { context.close(); }
        }
    }
    @Test void failedSessionDoesNotStarveLaterIdsOrRemoteCleanup() {
        var sessions = mock(VmSessionRepository.class); var service = mock(VmService.class); var docker = mock(DockerService.class);
        var recovery = new VmRecovery(sessions, service, docker);
        var a = VmSession.creating(7L, 9L, LocalDateTime.now()); ReflectionTestUtils.setField(a, "id", 10L);
        var b = VmSession.creating(8L, 9L, LocalDateTime.now()); ReflectionTestUtils.setField(b, "id", 11L);
        when(sessions.findByStatusInAndIdGreaterThanOrderByIdAsc(any(), eq(0L), any())).thenReturn(List.of(a, b));
        when(sessions.findByStatusInAndIdGreaterThanOrderByIdAsc(any(), eq(11L), any())).thenReturn(List.of());
        doThrow(new IllegalStateException()).when(service).reconcile(10L);
        recovery.reconcile(); recovery.reconcile(); recovery.reconcile();
        verify(service, times(2)).reconcile(10L); verify(service, times(2)).reconcile(11L);
        verify(docker, times(3)).reconcile();
    }
}

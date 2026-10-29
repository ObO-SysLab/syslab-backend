package net.diveon.backend.domain.vm.service;

import net.diveon.backend.domain.vm.repository.VmSessionRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.LoggerFactory;

@Component
@Profile("prod")
@ConditionalOnProperty(name = "vm.recovery.enabled", havingValue = "true")
public class VmRecovery {
    private final VmSessionRepository sessions;
    private final VmService service;
    private final DockerService docker;
    private long cursor;
    public VmRecovery(VmSessionRepository sessions, VmService service, DockerService docker) {
        this.sessions = sessions; this.service = service; this.docker = docker;
    }
    @Scheduled(fixedDelayString = "${vm.recovery.poll-ms:30000}")
    public void reconcile() {
        // Rotate bounded batches so one failing session cannot starve later sessions.
        var batch = sessions.findByStatusInAndIdGreaterThanOrderByIdAsc(VmSessionStore.ACTIVE, cursor, PageRequest.of(0, 20));
        if (batch.isEmpty()) cursor = 0;
        for (var session : batch) {
            cursor = session.getId();
            try { service.reconcile(session.getId()); }
            catch (RuntimeException e) { LoggerFactory.getLogger(VmRecovery.class).warn("VM reconciliation pending: session={}", session.getId()); }
        }
        try { docker.reconcile(); }
        catch (RuntimeException e) { LoggerFactory.getLogger(VmRecovery.class).warn("Remote VM reconciliation pending"); }
    }
}

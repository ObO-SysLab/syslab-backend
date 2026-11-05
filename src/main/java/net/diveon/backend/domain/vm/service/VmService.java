package net.diveon.backend.domain.vm.service;

import java.time.*;
import net.diveon.backend.domain.problem.repository.ProblemPracticeRepository;
import net.diveon.backend.domain.problem.service.ProblemAccessService;
import net.diveon.backend.domain.vm.dto.request.*;
import net.diveon.backend.domain.vm.dto.response.*;
import net.diveon.backend.domain.vm.entity.VmSession;
import net.diveon.backend.domain.vm.repository.VmSessionRepository;
import net.diveon.backend.global.exception.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class VmService {
    private final VmSessionRepository sessions;
    private final ProblemPracticeRepository practices;
    private final DockerService docker;
    private final ProblemAccessService access;
    private final VmSessionStore store;
    private final Clock clock;

    @Autowired
    public VmService(VmSessionRepository sessions, ProblemPracticeRepository practices, DockerService docker,
            ProblemAccessService access, VmSessionStore store) {
        this(sessions, practices, docker, access, store, Clock.systemDefaultZone());
    }
    VmService(VmSessionRepository sessions, ProblemPracticeRepository practices, DockerService docker,
            ProblemAccessService access, VmSessionStore store, Clock clock) {
        this.sessions = sessions; this.practices = practices; this.docker = docker;
        this.access = access; this.store = store; this.clock = clock;
    }
    private LocalDateTime now() { return LocalDateTime.now(clock); }
    private boolean expired(VmSession session) {
        return session.getExpiresAt() == null || !session.getExpiresAt().isAfter(now());
    }
    private DockerService.Target target(VmSession s) {
        if (s.getRuntimeVersion() != 1 && (s.getRuntimeVersion() != 0 || s.getContainerId() == null
                || !s.getContainerId().matches("[a-f0-9]{64}"))) throw unavailable();
        long expiry = s.getExpiresAt() == null ? 0 : s.getExpiresAt().atZone(clock.getZone()).toInstant().toEpochMilli();
        return new DockerService.Target(s.getId(), s.getUserId(), s.getProbId(), expiry,
                s.getRuntimeVersion() == 0 ? s.getContainerId() : null);
    }
    private String image(Long userId, Long problemId) {
        access.requirePractice(userId, problemId);
        var practice = practices.findById(problemId).orElseThrow(ProblemNotFoundException::new);
        if (!"READY".equals(practice.getImageStatus())) throw new ImageNotReadyException("실습 이미지가 준비되지 않았습니다.");
        return practice.getEcrImageUri();
    }
    public VmCreateResponse createVm(Long userId, VmCreateRequest request) {
        return create(userId, request.getProbId(), true);
    }
    private VmCreateResponse create(Long userId, Long problemId, boolean allowSwitch) {
        String image = image(userId, problemId);
        var reservation = store.reserve(userId, problemId, now()); // Commits before Docker.
        if (!reservation.created()) {
            var current = reservation.session();
            if (!allowSwitch) throw VmSessionStore.busy(); // An old reset must never stop a newer session.
            if (current.getProbId().equals(problemId) && !expired(current)) {
                if (running(current)) {
                    var fresh = store.exact(userId, problemId, current.getId());
                    if (!"RUNNING".equals(fresh.getStatus()) || expired(fresh)) throw VmSessionStore.busy();
                    return VmCreateResponse.from(fresh, "기존 VM을 반환합니다.");
                }
            }
            stop(current);
            reservation = store.reserve(userId, problemId, now());
            if (!reservation.created()) throw VmSessionStore.busy();
        }
        var session = reservation.session();
        try {
            String container = docker.runContainer(image, target(session));
            if (container == null || !container.matches("[a-f0-9]{64}")) throw unavailable();
            var running = store.activate(userId, problemId, session.getId(), container, now());
            return VmCreateResponse.from(running, "VM이 생성되었습니다.");
        } catch (RuntimeException failure) {
            // Even a lost run response has a committed reservation and deterministic remote identity.
            try { stop(session); } catch (RuntimeException cleanupFailed) {
                // CREATING/STOPPING remains durable; the recovery worker retries without a new create.
            }
            throw unavailable();
        }
    }
    public VmStatusResponse getStatus(Long userId, Long problemId) {
        access.requireRead(userId, problemId);
        var practice = practices.findById(problemId).orElseThrow(ProblemNotFoundException::new);
        if (!"READY".equals(practice.getImageStatus())) return VmStatusResponse.imageNotReady();
        var active = sessions.findAllByUserIdAndStatusIn(userId, VmSessionStore.ACTIVE);
        if (active.size() > 1) throw VmSessionStore.busy();
        if (active.isEmpty() || !active.getFirst().getProbId().equals(problemId)) return VmStatusResponse.none();
        var current = active.getFirst();
        if (!"RUNNING".equals(current.getStatus())) return VmStatusResponse.from(current);
        if (expired(current) || !running(current)) {
            stop(current); return VmStatusResponse.none();
        }
        var fresh = store.exact(userId, problemId, current.getId());
        return "RUNNING".equals(fresh.getStatus()) && !expired(fresh)
                ? VmStatusResponse.from(fresh) : VmStatusResponse.none();
    }
    public VmStopResponse stopVm(Long userId, VmStopRequest request) {
        stop(store.exact(userId, request.getProbId(), request.getSessionId()));
        return new VmStopResponse("success", "VM이 종료되었습니다.");
    }
    public VmCreateResponse resetVm(Long userId, VmStopRequest request) {
        image(userId, request.getProbId()); // Deny before touching any current session.
        var current = store.beginReset(userId, request.getProbId(), request.getSessionId());
        remove(current);
        return create(userId, request.getProbId(), false);
    }
    private void stop(VmSession session) {
        var stopping = store.stopping(session.getUserId(), session.getProbId(), session.getId());
        if (!"STOPPED".equals(stopping.getStatus())) remove(stopping);
    }
    private void remove(VmSession session) {
        try { docker.stopAndRemoveContainer(target(session)); }
        catch (RuntimeException e) { throw unavailable(); }
        store.stopped(session.getUserId(), session.getProbId(), session.getId());
    }
    private DockerService.State inspect(VmSession session) {
        try { return docker.inspect(target(session)); }
        catch (RuntimeException e) { throw unavailable(); } // Unknown is not missing.
    }
    private boolean running(VmSession session) {
        var actual = inspect(session);
        if ("RUNNING".equals(actual.status()) && !java.util.Objects.equals(actual.containerId(), session.getContainerId()))
            throw unavailable();
        return "RUNNING".equals(actual.status());
    }
    public void reconcile(Long sessionId) {
        var session = sessions.findById(sessionId).orElse(null);
        if (session == null || !VmSessionStore.ACTIVE.contains(session.getStatus())) return;
        if ("STOPPING".equals(session.getStatus())) { stop(session); return; }
        if ("CREATING".equals(session.getStatus())) {
            if (session.getCreatedAt().plusMinutes(2).isAfter(now())) return;
            stop(session); return;
        }
        boolean denied = false;
        try { access.requirePractice(session.getUserId(), session.getProbId()); }
        catch (ProblemNotFoundException noAccess) { denied = true; }
        if (denied || expired(session) || !running(session)) stop(session);
    }
    private static ResponseStatusException unavailable() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "VM 상태를 확정할 수 없습니다. 정리 후 다시 시도해주세요.");
    }
}

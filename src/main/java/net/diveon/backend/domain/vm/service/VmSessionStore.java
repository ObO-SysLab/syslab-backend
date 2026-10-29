package net.diveon.backend.domain.vm.service;

import java.time.LocalDateTime;
import java.util.List;
import net.diveon.backend.domain.user.repository.UserRepository;
import net.diveon.backend.domain.vm.entity.VmSession;
import net.diveon.backend.domain.vm.repository.VmSessionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Short committed intents; never holds a DB transaction across SSH/Docker calls. */
@Service
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class VmSessionStore {
    public static final List<String> ACTIVE = List.of("CREATING", "RUNNING", "STOPPING");
    private final UserRepository users;
    private final VmSessionRepository sessions;
    public VmSessionStore(UserRepository users, VmSessionRepository sessions) {
        this.users = users; this.sessions = sessions;
    }
    public record Reservation(VmSession session, boolean created) {}
    private void lock(Long userId) {
        users.lockVmOwner(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
    public Reservation reserve(Long userId, Long problemId, LocalDateTime now) {
        lock(userId);
        var active = sessions.findAllByUserIdAndStatusIn(userId, ACTIVE);
        if (active.size() > 1) throw busy(); // Legacy duplicates require explicit reconciliation.
        if (!active.isEmpty()) {
            var current = active.getFirst();
            if (!"RUNNING".equals(current.getStatus())) throw busy();
            return new Reservation(current, false);
        }
        return new Reservation(sessions.saveAndFlush(VmSession.creating(userId, problemId, now)), true);
    }
    public VmSession activate(Long userId, Long problemId, Long id, String containerId, LocalDateTime now) {
        lock(userId);
        var session = exact(userId, problemId, id);
        if (!"CREATING".equals(session.getStatus()) || session.getExpiresAt() == null
                || !session.getExpiresAt().isAfter(now)) throw busy();
        session.running(containerId);
        return sessions.saveAndFlush(session);
    }
    public VmSession stopping(Long userId, Long problemId, Long id) {
        lock(userId);
        var session = exact(userId, problemId, id);
        if ("STOPPED".equals(session.getStatus())) return session;
        if (!ACTIVE.contains(session.getStatus())) throw busy();
        session.stopping();
        return sessions.saveAndFlush(session);
    }
    public VmSession beginReset(Long userId, Long problemId, Long id) {
        lock(userId);
        var session = exact(userId, problemId, id);
        if (!"RUNNING".equals(session.getStatus())) throw busy();
        session.stopping();
        return sessions.saveAndFlush(session);
    }
    public void stopped(Long userId, Long problemId, Long id) {
        lock(userId);
        var session = exact(userId, problemId, id);
        if (!"STOPPING".equals(session.getStatus()) && !"STOPPED".equals(session.getStatus())) throw busy();
        session.stop(); sessions.saveAndFlush(session);
    }
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public VmSession exact(Long userId, Long problemId, Long id) {
        if (id == null || id <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "세션 ID가 필요합니다.");
        return sessions.findByIdAndUserIdAndProbId(id, userId, problemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "세션을 찾을 수 없습니다."));
    }
    public static ResponseStatusException busy() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "VM 생성 또는 정리가 진행 중입니다. 잠시 후 다시 시도해주세요.");
    }
}

package net.diveon.backend.domain.vm.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Objects;
import net.diveon.backend.domain.vm.entity.VmSession;
import net.diveon.backend.domain.vm.repository.VmSessionRepository;
import net.diveon.backend.domain.problem.service.ProblemAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class TerminalAccessService {
    private final VmSessionRepository sessions;
    private final TerminalGrantStore store;
    private final String serviceKey;
    private final Clock clock;
    private final ProblemAccessService access;
    private final SecureRandom random = new SecureRandom();

    @Autowired
    public TerminalAccessService(VmSessionRepository sessions, TerminalGrantStore store,
            @Value("${terminal.service-key:}") String serviceKey, ProblemAccessService access) {
        this(sessions, store, serviceKey, Clock.systemDefaultZone(), access);
    }

    TerminalAccessService(VmSessionRepository sessions, TerminalGrantStore store, String key, Clock clock,
            ProblemAccessService access) {
        this.sessions = sessions; this.store = store; this.serviceKey = key; this.clock = clock;
        this.access = access;
    }

    public record Ticket(String ticket, long expiresAt) {}
    public record Connection(String lease, String containerId, long expiresAt) {}
    public record Status(String containerId, long expiresAt) {}
    private record Grant(long sessionId, long userId, String containerId, long expiresAt, long validUntil) {
        String encode() { return sessionId + ":" + userId + ":" + containerId + ":" + expiresAt + ":" + validUntil; }
    }

    public Ticket issue(Long userId, Long probId, String containerId) {
        configured();
        if (userId == null || probId == null || probId <= 0 || !validContainer(containerId)) throw denied();
        VmSession session = sessions.findFirstByUserIdAndProbIdAndStatus(userId, probId, "RUNNING")
                .orElseThrow(TerminalAccessService::denied);
        active(session, userId, containerId);
        long expiresAt = expiry(session);
        long validUntil = Math.min(clock.millis() + 30_000, expiresAt);
        String ticket = token();
        store.put("ticket", ticket, new Grant(session.getId(), userId, containerId, expiresAt, validUntil).encode(),
                Duration.ofMillis(validUntil - clock.millis()));
        return new Ticket(ticket, validUntil);
    }

    public Connection consume(String key, String ticket, String containerId) {
        authenticateServer(key);
        validateToken(ticket);
        if (!validContainer(containerId)) throw denied();
        Grant grant = decode(store.consume(ticket));
        if (!grant.containerId().equals(containerId)) throw denied();
        validateSession(grant);
        String lease = token();
        Grant leaseGrant = new Grant(grant.sessionId(), grant.userId(), containerId, grant.expiresAt(), grant.expiresAt());
        store.put("lease", lease, leaseGrant.encode(), Duration.ofMillis(grant.expiresAt() - clock.millis()));
        return new Connection(lease, containerId, grant.expiresAt());
    }

    public Status check(String key, String lease) {
        authenticateServer(key);
        validateToken(lease);
        Grant grant = decode(store.lease(lease));
        validateSession(grant);
        return new Status(grant.containerId(), grant.expiresAt());
    }

    private void validateSession(Grant grant) {
        if (grant.validUntil() <= clock.millis() || grant.expiresAt() <= clock.millis()) throw denied();
        VmSession session = sessions.findById(grant.sessionId()).orElseThrow(TerminalAccessService::denied);
        active(session, grant.userId(), grant.containerId());
        if (expiry(session) != grant.expiresAt()) throw denied();
    }

    private void active(VmSession session, Long userId, String containerId) {
        if (!Objects.equals(userId, session.getUserId()) || !Objects.equals(containerId, session.getContainerId())
                || !"RUNNING".equals(session.getStatus()) || session.getExpiresAt() == null
                || !session.getExpiresAt().isAfter(LocalDateTime.now(clock))) throw denied();
        access.requirePractice(userId, session.getProbId());
    }

    private long expiry(VmSession session) {
        // Existing VM timestamps are LocalDateTime in the API host's timezone.
        return session.getExpiresAt().atZone(clock.getZone()).toInstant().toEpochMilli();
    }

    private void configured() {
        if (serviceKey == null || serviceKey.length() < 32) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Terminal access unavailable");
        }
    }

    private void authenticateServer(String key) {
        configured();
        if (key == null || key.length() > 256 || !MessageDigest.isEqual(
                serviceKey.getBytes(StandardCharsets.UTF_8), key.getBytes(StandardCharsets.UTF_8))) throw denied();
    }

    private static boolean validContainer(String id) { return id != null && id.matches("[a-f0-9]{64}"); }
    private static void validateToken(String token) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) throw denied();
    }
    private String token() {
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    private static Grant decode(String value) {
        if (value == null) throw denied();
        try {
            String[] fields = value.split(":");
            if (fields.length != 5 || !validContainer(fields[2])) throw denied();
            return new Grant(Long.parseLong(fields[0]), Long.parseLong(fields[1]), fields[2],
                    Long.parseLong(fields[3]), Long.parseLong(fields[4]));
        } catch (IllegalArgumentException ex) { throw denied(); }
    }
    private static ResponseStatusException denied() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, "Terminal access denied");
    }
}

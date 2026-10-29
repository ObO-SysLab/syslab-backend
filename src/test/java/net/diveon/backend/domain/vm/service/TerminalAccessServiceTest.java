package net.diveon.backend.domain.vm.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import net.diveon.backend.domain.vm.entity.VmSession;
import net.diveon.backend.domain.vm.repository.VmSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

class TerminalAccessServiceTest {
    static final String KEY = "test-only-server-key-0123456789abcdef";
    static final String CID = "a".repeat(64);
    VmSessionRepository sessions;
    TerminalGrantStore store;
    TerminalAccessService service;
    VmSession session;
    Map<String, String> values;
    Clock clock = Clock.fixed(Instant.parse("2026-10-29T12:00:00Z"), ZoneId.of("Asia/Seoul"));

    @BeforeEach void setup() {
        sessions = mock(VmSessionRepository.class); store = mock(TerminalGrantStore.class);
        service = new TerminalAccessService(sessions, store, KEY, clock);
        session = new VmSession(7L, 9L, CID, LocalDateTime.now(clock), LocalDateTime.now(clock).plusHours(2));
        ReflectionTestUtils.setField(session, "id", 3L);
        when(sessions.findFirstByUserIdAndProbIdAndStatus(7L, 9L, "RUNNING")).thenReturn(Optional.of(session));
        when(sessions.findById(3L)).thenAnswer(call -> Optional.of(session));
        values = new ConcurrentHashMap<>();
        doAnswer(call -> { values.put(call.getArgument(0) + ":" + call.getArgument(1), call.getArgument(2)); return null; })
                .when(store).put(anyString(), anyString(), anyString(), any(Duration.class));
        when(store.consume(anyString())).thenAnswer(call -> values.remove("ticket:" + call.getArgument(0)));
        when(store.lease(anyString())).thenAnswer(call -> values.get("lease:" + call.getArgument(0)));
    }
    String issue() { return service.issue(7L, 9L, CID).ticket(); }

    @Test void ownerConnectsAndRechecksWithSeparateLease() {
        var ticket = service.issue(7L, 9L, CID);
        assertEquals(clock.millis() + 30000, ticket.expiresAt());
        var connection = service.consume(KEY, ticket.ticket(), CID);
        assertNotEquals(ticket.ticket(), connection.lease());
        assertEquals(CID, service.check(KEY, connection.lease()).containerId());
        assertEquals(clock.millis() + 7200000, connection.expiresAt());
        verify(store).put(eq("ticket"), anyString(), anyString(), eq(Duration.ofSeconds(30)));
    }
    @Test void otherUserProblemOrContainerCannotIssue() {
        assertThrows(ResponseStatusException.class, () -> service.issue(8L, 9L, CID));
        assertThrows(ResponseStatusException.class, () -> service.issue(7L, 10L, CID));
        assertThrows(ResponseStatusException.class, () -> service.issue(7L, 9L, "b".repeat(64)));
        verify(store, never()).put(any(), any(), any(), any());
    }
    @Test void expiredNullExpiryAndStoppedCannotIssue() {
        session.stop(); assertThrows(ResponseStatusException.class, this::issue);
        ReflectionTestUtils.setField(session, "status", "RUNNING");
        ReflectionTestUtils.setField(session, "expiresAt", LocalDateTime.now(clock));
        assertThrows(ResponseStatusException.class, this::issue);
        ReflectionTestUtils.setField(session, "expiresAt", null);
        assertThrows(ResponseStatusException.class, this::issue);
    }
    @ParameterizedTest @ValueSource(strings = {"", "forged", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"})
    void missingMalformedOrForgedTicketDenied(String ticket) {
        assertThrows(ResponseStatusException.class, () -> service.consume(KEY, ticket, CID));
    }
    @Test void missingWrongOrLoginCredentialCannotConsume() {
        String ticket = issue();
        for (String key : Arrays.asList(null, "", "login-token", KEY + "x")) {
            assertThrows(ResponseStatusException.class, () -> service.consume(key, ticket, CID));
        }
        verify(store, never()).consume(any());
        assertNotNull(service.consume(KEY, ticket, CID));
    }
    @Test void missingConfigurationFailsClosed() {
        service = new TerminalAccessService(sessions, store, "", clock);
        assertEquals(503, assertThrows(ResponseStatusException.class, this::issue).getStatusCode().value());
    }
    @Test void wrongTargetBurnsTicketAndReplayIsDenied() {
        String ticket = issue();
        assertThrows(ResponseStatusException.class, () -> service.consume(KEY, ticket, "b".repeat(64)));
        assertThrows(ResponseStatusException.class, () -> service.consume(KEY, ticket, CID));
    }
    @Test void ticketCannotBeUsedAsLeaseOrLeaseAsTicket() {
        String ticket = issue();
        assertThrows(ResponseStatusException.class, () -> service.check(KEY, ticket));
        var grant = service.consume(KEY, ticket, CID);
        assertThrows(ResponseStatusException.class, () -> service.consume(KEY, grant.lease(), CID));
        assertThrows(ResponseStatusException.class, () -> service.consume(KEY, ticket, CID));
    }
    @Test void expiredTicketDeniedEvenIfRedisStillReturnsIt() {
        String ticket = issue();
        service = new TerminalAccessService(sessions, store, KEY, Clock.offset(clock, Duration.ofSeconds(30)));
        assertThrows(ResponseStatusException.class, () -> service.consume(KEY, ticket, CID));
    }
    @Test void stoppedAfterIssueDeniedBeforeConnection() {
        String ticket = issue(); session.stop();
        assertThrows(ResponseStatusException.class, () -> service.consume(KEY, ticket, CID));
    }
    @Test void openLeaseDeniedAfterStopExpiryOwnerOrContainerChange() {
        for (String field : List.of("status", "expiresAt", "userId", "containerId")) {
            setup(); var grant = service.consume(KEY, issue(), CID);
            switch (field) {
                case "status" -> session.stop();
                case "expiresAt" -> ReflectionTestUtils.setField(session, field, LocalDateTime.now(clock));
                case "userId" -> ReflectionTestUtils.setField(session, field, 8L);
                case "containerId" -> ReflectionTestUtils.setField(session, field, "b".repeat(64));
            }
            assertThrows(ResponseStatusException.class, () -> service.check(KEY, grant.lease()));
        }
    }
    @Test void leaseExpiresEvenIfSessionIsExtended() {
        var grant = service.consume(KEY, issue(), CID);
        service = new TerminalAccessService(sessions, store, KEY, Clock.offset(clock, Duration.ofHours(2)));
        assertThrows(ResponseStatusException.class, () -> service.check(KEY, grant.lease()));
    }
    @Test void redisAndDatabaseOutagesDoNotIssueAccess() {
        doThrow(new IllegalStateException("redis down")).when(store).put(any(), any(), any(), any());
        assertThrows(IllegalStateException.class, this::issue);
        when(sessions.findFirstByUserIdAndProbIdAndStatus(7L, 9L, "RUNNING")).thenThrow(new IllegalStateException("db down"));
        assertThrows(IllegalStateException.class, this::issue);
    }
    @Test void concurrentConsumeOnlyOneSucceeds() throws Exception {
        String ticket = issue();
        try (var pool = Executors.newFixedThreadPool(2)) {
            Callable<Boolean> consume = () -> {
                try { service.consume(KEY, ticket, CID); return true; }
                catch (ResponseStatusException ex) { return false; }
            };
            var results = pool.invokeAll(List.of(consume, consume));
            assertEquals(1, results.stream().filter(f -> { try { return f.get(); } catch (Exception ex) { throw new RuntimeException(ex); } }).count());
        }
    }
}

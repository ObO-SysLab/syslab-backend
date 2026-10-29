package net.diveon.backend.domain.grade.service;

import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.nio.file.*;
import java.net.ServerSocket;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real SQL and transaction boundaries, using a disposable loopback PostgreSQL, never application config. */
@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "DIVEON_GRADING_PG_TEST", matches = "approved")
class CodingGradeJobStoreTest {
    static Path directory, pgBin;
    static String suffix;
    static JdbcTemplate jdbc;
    static TransactionTemplate tx;
    static CodingGradeJobStore store;

    static void command(String... command) throws Exception {
        var p = new ProcessBuilder(command).redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.appendTo(directory.resolve("process.log").toFile())).start();
        if (!p.waitFor(45, TimeUnit.SECONDS)) { p.destroyForcibly(); fail("PostgreSQL command timeout"); }
        assertEquals(0, p.exitValue(), "See " + directory.resolve("process.log"));
    }

    @BeforeAll static void start() throws Exception {
        String binaryPath = System.getenv("PG_BIN");
        assertNotNull(binaryPath, "Set PG_BIN explicitly for the approved local PostgreSQL installation");
        pgBin = Path.of(binaryPath);
        suffix = System.getProperty("os.name").startsWith("Windows") ? ".exe" : "";
        directory = Files.createTempDirectory("syslab-recovery-pg-");
        int port;
        try (var socket = new ServerSocket(0)) { port = socket.getLocalPort(); }
        command(pgBin.resolve("initdb" + suffix).toString(), "-D", directory.resolve("data").toString(),
                "-U", "postgres", "-A", "trust", "--no-locale", "-E", "UTF8");
        command(pgBin.resolve("pg_ctl" + suffix).toString(), "-D", directory.resolve("data").toString(),
                "-l", directory.resolve("server.log").toString(), "-o", "-h 127.0.0.1 -p " + port, "-w", "start");
        var ds = new DriverManagerDataSource("jdbc:postgresql://127.0.0.1:" + port + "/postgres", "postgres", "");
        jdbc = new JdbcTemplate(ds);
        tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
        store = new CodingGradeJobStore(jdbc);
        jdbc.execute("CREATE TABLE solve_submission (id bigint PRIMARY KEY, submission_state text NOT NULL)");
        jdbc.execute("CREATE TABLE contest_submission (submission_id bigint, submission_status text NOT NULL)");
        jdbc.execute(Files.readString(Path.of("ops/2026-10-29-coding-grade-recovery.sql")));
    }

    @AfterAll static void stop() throws Exception {
        if (directory != null && Files.exists(directory.resolve("data/postmaster.pid"))) {
            command(pgBin.resolve("pg_ctl" + suffix).toString(), "-D", directory.resolve("data").toString(), "-m", "immediate", "-w", "stop");
        }
        System.out.println("Disposable PostgreSQL stopped; logs: " + directory);
    }

    @BeforeEach void seed() {
        jdbc.execute("TRUNCATE coding_grade_job, solve_submission, contest_submission");
        jdbc.update("INSERT INTO solve_submission VALUES (101, 'PENDING')");
        jdbc.update("INSERT INTO contest_submission VALUES (101, 'PENDING')");
        tx.executeWithoutResult(s -> store.register(101, "{}", 60));
    }

    CodingGradeJobStore.Dispatch claim() { return tx.execute(s -> store.claimNext(3)); }
    void expire() { jdbc.update("UPDATE coding_grade_job SET deadline_at = clock_timestamp() - interval '1 second'"); }

    @Test void threeAttemptsThenBothStatusesFailWithoutCreatingVerdict() {
        for (int n = 1; n <= 3; n++) {
            assertEquals(n, claim().attempt());
            assertNull(claim());
            expire();
        }
        assertNull(claim().payload());
        assertEquals("FAILED", jdbc.queryForObject("SELECT submission_state FROM solve_submission", String.class));
        assertEquals("FAILED", jdbc.queryForObject("SELECT submission_status FROM contest_submission", String.class));
        assertNull(claim());
    }

    @Test void completedSubmissionDoesNotRetry() {
        jdbc.update("UPDATE solve_submission SET submission_state = 'COMPLETED'");
        assertNull(claim().payload());
        assertEquals(0, jdbc.queryForObject("SELECT attempt_no FROM coding_grade_job", Integer.class));
    }

    @Test void rolledBackRequestDoesNotSurvive() {
        tx.executeWithoutResult(status -> {
            jdbc.update("INSERT INTO solve_submission VALUES (102, 'PENDING')");
            store.register(102, "{}", 60);
            status.setRollbackOnly();
        });
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM coding_grade_job", Integer.class));
    }

    @Test void concurrentDispatchersClaimOnlyOnce() throws Exception {
        try (var pool = Executors.newFixedThreadPool(2)) {
            var a = pool.submit(this::claim);
            var b = pool.submit(this::claim);
            int claims = (a.get(10, TimeUnit.SECONDS) != null ? 1 : 0) + (b.get(10, TimeUnit.SECONDS) != null ? 1 : 0);
            assertEquals(1, claims);
        }
    }

    @Test void lateFailureCannotExpireNewAttempt() {
        var old = claim();
        expire();
        assertEquals(2, claim().attempt());
        tx.executeWithoutResult(s -> store.dispatchFailed(old));
        assertNull(claim());
    }

    @Test void failedSendCanRetryAndDuplicateRegistrationDoesNotResetAttempts() {
        var first = claim();
        tx.executeWithoutResult(s -> store.dispatchFailed(first));
        tx.executeWithoutResult(s -> store.register(101, "changed", 90));
        var second = claim();
        assertEquals(2, second.attempt());
        assertEquals("{}", second.payload());
    }

    @Test void failedAcceptanceRollsBackSubmissionAndOutboxTogether() {
        var starter = org.mockito.Mockito.mock(SubmissionGradeStarterService.class);
        var grader = org.mockito.Mockito.mock(SubmissionGradeAsyncService.class);
        var request = new net.diveon.backend.domain.grade.dto.request.SubmissionGradeReqeust();
        org.springframework.test.util.ReflectionTestUtils.setField(request, "prodId", 2L);
        org.mockito.Mockito.when(starter.submissionGradeStart(7L, request)).thenAnswer(call -> {
            jdbc.update("INSERT INTO solve_submission VALUES (102, 'PENDING')");
            return new net.diveon.backend.domain.grade.dto.response.SubmissionGradeResponse(102, 2, "PENDING", "coding");
        });
        org.mockito.Mockito.doAnswer(call -> {
            store.register(102, "{}", 60);
            throw new IllegalStateException("Injected acceptance failure");
        }).when(grader).gradeProblemCoding(2, 7, 102);
        var service = new SubmissionGradeService(starter, grader, tx.getTransactionManager(), true);
        assertThrows(IllegalStateException.class, () -> service.submit(7L, request));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM solve_submission WHERE id = 102", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM coding_grade_job WHERE submission_id = 102", Integer.class));
    }

    @Test void failureStateAndJobFinishRollBackTogether() {
        jdbc.update("UPDATE coding_grade_job SET attempt_no = 3");
        jdbc.execute("ALTER TABLE contest_submission ADD CONSTRAINT injected_failure CHECK (submission_status <> 'FAILED')");
        try {
            assertThrows(org.springframework.dao.DataIntegrityViolationException.class, this::claim);
            assertEquals("PENDING", jdbc.queryForObject("SELECT submission_state FROM solve_submission", String.class));
            assertNull(jdbc.queryForObject("SELECT finished_at FROM coding_grade_job", java.sql.Timestamp.class));
        } finally {
            jdbc.execute("ALTER TABLE contest_submission DROP CONSTRAINT injected_failure");
        }
    }
}

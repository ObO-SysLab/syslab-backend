package net.diveon.backend.domain.vm.service;

import static org.junit.jupiter.api.Assertions.*;
import net.diveon.backend.support.TestPorts;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import javax.sql.DataSource;
import jakarta.persistence.EntityManagerFactory;
import net.diveon.backend.domain.user.entity.User;
import net.diveon.backend.domain.user.repository.UserRepository;
import net.diveon.backend.domain.vm.entity.VmSession;
import net.diveon.backend.domain.vm.repository.VmSessionRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

/** Explicit opt-in only. Starts an isolated local DB; never loads application config or contacts AWS/SSH. */
@EnabledIfEnvironmentVariable(named = "DIVEON_VM_PG_TEST", matches = "approved")
class VmSessionPostgresTest {
    static Path directory, pgBin;
    static String executableSuffix;
    static DataSource dataSource;
    static AnnotationConfigApplicationContext context;
    static JdbcTemplate jdbc;
    static VmSessionStore store;
    static TransactionTemplate tx;
    static LocalDateTime now = LocalDateTime.of(2026, 10, 30, 0, 0);

    @Configuration @EnableTransactionManagement
    @EnableJpaRepositories(basePackageClasses = {UserRepository.class, VmSessionRepository.class})
    static class Config {
        @Bean DataSource dataSource() { return dataSource; }
        @Bean LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource ds) {
            var factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(ds); factory.setPackagesToScan(User.class.getPackageName(), VmSession.class.getPackageName());
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "create", "hibernate.jdbc.time_zone", "UTC"));
            return factory;
        }
        @Bean PlatformTransactionManager transactionManager(EntityManagerFactory factory) { return new JpaTransactionManager(factory); }
        @Bean VmSessionStore store(UserRepository users, VmSessionRepository sessions) { return new VmSessionStore(users, sessions); }
    }
    static void command(String... args) throws Exception {
        var process = new ProcessBuilder(args).redirectErrorStream(true)
            .redirectOutput(ProcessBuilder.Redirect.appendTo(directory.resolve("process.log").toFile())).start();
        if (!process.waitFor(45, TimeUnit.SECONDS)) { process.destroyForcibly(); fail("Local PostgreSQL command timed out; inspect " + directory); }
        assertEquals(0, process.exitValue(), "Inspect " + directory.resolve("process.log"));
    }
    @BeforeAll static void start() throws Exception {
        String binaryPath = System.getenv("PG_BIN");
        assertNotNull(binaryPath, "Set PG_BIN explicitly for the approved local PostgreSQL installation");
        pgBin = Path.of(binaryPath); executableSuffix = System.getProperty("os.name").startsWith("Windows") ? ".exe" : "";
        directory = Files.createTempDirectory("diveon-vm-pg-").toRealPath();
        assertTrue(directory.startsWith(Path.of(System.getProperty("java.io.tmpdir")).toRealPath()));
        int port = TestPorts.freePort();
        try {
            command(pgBin.resolve("initdb" + executableSuffix).toString(), "-D", directory.resolve("data").toString(),
                "-U", "postgres", "-A", "trust", "--no-locale", "-E", "UTF8");
            command(pgBin.resolve("pg_ctl" + executableSuffix).toString(), "-D", directory.resolve("data").toString(),
                "-l", directory.resolve("server.log").toString(), "-o", "-h 127.0.0.1 -p " + port, "-w", "start");
            dataSource = new DriverManagerDataSource("jdbc:postgresql://127.0.0.1:" + port + "/postgres", "postgres", "");
            context = new AnnotationConfigApplicationContext(Config.class);
            jdbc = new JdbcTemplate(dataSource); store = context.getBean(VmSessionStore.class);
            tx = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
            jdbc.execute("ALTER TABLE vm_sessions DROP COLUMN runtime_version"); // legacy shape
            jdbc.execute(Files.readString(Path.of("ops/2026-10-30-vm-lifecycle.sql")));
        } catch (Throwable failure) { stop(); throw failure; }
    }
    @AfterAll static void stop() throws Exception {
        if (context != null) { context.close(); context = null; }
        if (directory != null && Files.exists(directory.resolve("data/postmaster.pid")))
            command(pgBin.resolve("pg_ctl" + executableSuffix).toString(), "-D", directory.resolve("data").toString(), "-m", "fast", "-w", "stop");
        if (directory != null) System.out.println("Disposable VM PostgreSQL stopped; retained logs: " + directory);
    }
    @BeforeEach void seed() {
        jdbc.execute("TRUNCATE vm_sessions, domain_user RESTART IDENTITY");
        tx.executeWithoutResult(s -> context.getBean(UserRepository.class).saveAndFlush(new User("local", "unused", "local", "local@example.test", null, null)));
    }
    @Test void concurrentReservationsCommitOnlyOneActiveSession() throws Exception {
        var start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            Callable<Boolean> reserve = () -> { start.await(); try { return store.reserve(1L, 9L, now).created(); }
                catch (ResponseStatusException busy) { assertEquals(409, busy.getStatusCode().value()); return false; } };
            var a = pool.submit(reserve); var b = pool.submit(reserve); start.countDown();
            assertNotEquals(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS));
        }
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM vm_sessions", Integer.class));
    }
    @Test void creationIntentSurvivesCallerRollback() {
        tx.executeWithoutResult(outer -> { store.reserve(1L, 9L, now); outer.setRollbackOnly(); });
        assertEquals("CREATING", jdbc.queryForObject("SELECT status FROM vm_sessions", String.class));
    }
    @Test void stoppingSurvivesRollbackAndBlocksReplacementUntilConfirmed() {
        var vm = store.reserve(1L, 9L, now).session();
        tx.executeWithoutResult(outer -> { store.stopping(1L, 9L, vm.getId()); outer.setRollbackOnly(); });
        assertEquals("STOPPING", jdbc.queryForObject("SELECT status FROM vm_sessions", String.class));
        assertThrows(ResponseStatusException.class, () -> store.reserve(1L, 10L, now));
        store.stopped(1L, 9L, vm.getId()); assertTrue(store.reserve(1L, 10L, now).created());
    }
    @Test void databaseConstraintAlsoBlocksWritersOutsideStore() {
        store.reserve(1L, 9L, now);
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> jdbc.update(
            "INSERT INTO vm_sessions(user_id,prob_id,status,created_at,runtime_version) VALUES(1,10,'CREATING',now(),1)"));
    }
    @Test void lateActivationAndOldStopCannotAffectReplacement() {
        var old = store.reserve(1L, 9L, now).session(); store.stopping(1L, 9L, old.getId()); store.stopped(1L, 9L, old.getId());
        var newer = store.reserve(1L, 9L, now).session();
        assertThrows(ResponseStatusException.class, () -> store.activate(1L, 9L, old.getId(), "a".repeat(64), now));
        store.stopping(1L, 9L, old.getId());
        assertEquals("CREATING", store.exact(1L, 9L, newer.getId()).getStatus());
    }
    @Test void actualRepositoryScopesOwnerAndProblem() {
        var vm = store.reserve(1L, 9L, now).session();
        assertThrows(ResponseStatusException.class, () -> store.exact(2L, 9L, vm.getId()));
        assertThrows(ResponseStatusException.class, () -> store.exact(1L, 10L, vm.getId()));
    }
}

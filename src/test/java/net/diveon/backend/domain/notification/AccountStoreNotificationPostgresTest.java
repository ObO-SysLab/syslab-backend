package net.diveon.backend.domain.notification;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import jakarta.persistence.EntityManager;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import net.diveon.backend.domain.activity.service.RecentActivityService;
import net.diveon.backend.domain.contest.entity.Contest;
import net.diveon.backend.domain.contest.entity.ContestParticipant;
import net.diveon.backend.domain.grade.entity.SolveResult;
import net.diveon.backend.domain.grade.entity.SolveSubmission;
import net.diveon.backend.domain.group.entity.Group;
import net.diveon.backend.domain.group.entity.GroupUser;
import net.diveon.backend.domain.group.repository.GroupUserRepository;
import net.diveon.backend.domain.group.service.GroupService;
import net.diveon.backend.domain.notification.dto.NotificationSettings;
import net.diveon.backend.domain.notification.service.NotificationService;
import net.diveon.backend.domain.problem.entity.Problem;
import net.diveon.backend.domain.store.service.PointService;
import net.diveon.backend.domain.store.service.StoreService;
import net.diveon.backend.domain.user.entity.User;
import net.diveon.backend.domain.user.repository.UserRepository;
import net.diveon.backend.domain.user.service.UserService;
import net.diveon.backend.domain.user.service.UserWithdrawService;
import net.diveon.backend.global.exception.DomainRuleException;
import net.diveon.backend.global.s3.ImageUploadService;
import net.diveon.backend.global.security.WithdrawnUsers;
import net.diveon.backend.support.TestPorts;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 2026-11-12 묶음 B: 운영과 같은 순서(기존 스키마 → ops SQL → validate)를 임시 PostgreSQL에서 재현하고
 * 상점·알림·탈퇴·탐사 로그의 네이티브 쿼리를 실제로 실행한다. 앱을 띄우지 않고 배포 설정도 읽지 않는다.
 */
@EnabledIfEnvironmentVariable(named = "OBO_PG_TEST", matches = "local")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AccountStoreNotificationPostgresTest {
    static Path directory, bin;
    static DriverManagerDataSource ds;
    static LocalContainerEntityManagerFactoryBean factory;
    static EntityManager em;
    static TransactionTemplate tx;
    static JdbcTemplate jdbc;
    static UserRepository users;
    static GroupUserRepository groupUsers;
    static PointService points;
    static StoreService store;
    static NotificationService notifications;
    static RecentActivityService activity;
    static final String suffix = System.getProperty("os.name").startsWith("Windows") ? ".exe" : "";

    static User author, solver, other;
    static Problem p1, p2, pGroup, pOwn, pContest, p6, yesterdayHard;
    static net.diveon.backend.domain.problem.repository.ProblemRepository problems;
    static net.diveon.backend.domain.grade.repository.SolveSubmissionRepository submissions;

    static void command(String... args) throws Exception {
        var process = new ProcessBuilder(args).redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.appendTo(directory.resolve("process.log").toFile())).start();
        if (!process.waitFor(180, TimeUnit.SECONDS)) { process.destroyForcibly(); fail("Local PostgreSQL timed out: " + directory); }
        assertEquals(0, process.exitValue(), "Inspect " + directory.resolve("process.log"));
    }

    static LocalContainerEntityManagerFactoryBean emf(String mode) {
        var f = new LocalContainerEntityManagerFactoryBean();
        f.setDataSource(ds);
        f.setPackagesToScan("net.diveon.backend.domain");
        f.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        // 운영과 같게 hibernate.jdbc.time_zone을 두지 않는다(JPA·JDBC 모두 서버 시간대의 LocalDateTime을 그대로 쓴다).
        f.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", mode,
                "hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy"));
        f.afterPropertiesSet();
        return f;
    }

    @BeforeAll
    static void start() throws Exception {
        assertNotNull(System.getenv("PG_BIN"));
        bin = Path.of(System.getenv("PG_BIN"));
        directory = Files.createTempDirectory("account-store-pg-").toRealPath();
        int port = TestPorts.freePort();
        try {
            command(bin.resolve("initdb" + suffix).toString(), "-D", directory.resolve("data").toString(), "-U", "postgres", "-A", "trust", "--no-locale", "-E", "UTF8");
            command(bin.resolve("pg_ctl" + suffix).toString(), "-D", directory.resolve("data").toString(), "-l", directory.resolve("server.log").toString(), "-o", "-h 127.0.0.1 -p " + port, "-w", "start");
            ds = new DriverManagerDataSource("jdbc:postgresql://127.0.0.1:" + port + "/postgres", "postgres", "");
            jdbc = new JdbcTemplate(ds);

            // 1) 운영의 현재 상태 재현: 엔티티 스키마에서 이번에 더하는 칼럼을 빼고, 기존 수동 테이블(problem_solver)을 만든다.
            var create = emf("create");
            create.destroy();
            jdbc.execute("ALTER TABLE domain_user DROP COLUMN deleted_at");
            jdbc.execute("CREATE TABLE IF NOT EXISTS problem_solver(prob_id bigint NOT NULL REFERENCES problem_summary(id) ON DELETE CASCADE, user_id bigint NOT NULL REFERENCES domain_user(id) ON DELETE CASCADE, PRIMARY KEY(prob_id,user_id))");

            // 2) 운영에 적용할 SQL을 두 번 실행해도 결과가 같아야 한다.
            String sql = Files.readString(Path.of("ops/2026-11-12-account-store-notification.sql"));
            jdbc.execute(sql);
            jdbc.execute(sql);

            // 3) 새 엔티티가 SQL 적용 뒤 스키마와 맞는지 운영처럼 validate로 확인한다.
            factory = emf("validate");
            var emf = factory.getObject();
            em = SharedEntityManagerCreator.createSharedEntityManager(emf);
            tx = new TransactionTemplate(new JpaTransactionManager(emf));
            var repositories = new JpaRepositoryFactory(em);
            users = repositories.getRepository(UserRepository.class);
            groupUsers = repositories.getRepository(GroupUserRepository.class);
            problems = repositories.getRepository(net.diveon.backend.domain.problem.repository.ProblemRepository.class);
            submissions = repositories.getRepository(net.diveon.backend.domain.grade.repository.SolveSubmissionRepository.class);
            points = new PointService(jdbc);
            store = new StoreService(jdbc, points, users);
            notifications = new NotificationService(jdbc);
            activity = new RecentActivityService(jdbc);
            seed();
        } catch (Throwable error) {
            try { stop(); } catch (Throwable cleanup) { error.addSuppressed(cleanup); }
            throw error;
        }
    }

    @AfterAll
    static void stop() throws Exception {
        if (factory != null) { factory.destroy(); factory = null; }
        if (directory != null && Files.exists(directory.resolve("data/postmaster.pid")))
            command(bin.resolve("pg_ctl" + suffix).toString(), "-D", directory.resolve("data").toString(), "-m", "fast", "-w", "stop");
    }

    static User user(String name) {
        var u = new User(name, "pw", name, name + "@example.test", null, null);
        em.persist(u);
        return u;
    }

    static Problem problem(User by, String difficulty, String visibility) {
        var p = new Problem(by, "objective", "문제 " + difficulty + visibility, "Memory", difficulty, visibility);
        em.persist(p);
        return p;
    }

    // 정답 제출 하나(제출 시각을 정해서). 포인트는 제출 시각의 서울 날짜로 하루 한도를 계산한다.
    static void solve(User u, Problem p, LocalDateTime at, SolveResult.SolveResultState state) {
        tx.executeWithoutResult(s -> {
            var sub = new SolveSubmission(p, u);
            em.persist(sub);
            em.persist(new SolveResult(sub, state));
            em.flush();
            jdbc.update("UPDATE solve_submission SET submitted_at = ? WHERE id = ?", java.sql.Timestamp.valueOf(at), sub.getId());
        });
    }

    static void seed() {
        tx.executeWithoutResult(s -> {
            author = user("author");
            solver = user("solver");
            other = user("other");
            p1 = problem(author, "1", "public");
            p2 = problem(author, "3", "public");
            pGroup = problem(author, "2", "group");
            pOwn = problem(solver, "2", "public");
            pContest = problem(author, "1", "contest");
            p6 = problem(author, "2", "public");
            em.flush();
        });
        LocalDateTime base = LocalDateTime.now().withSecond(0).withNano(0).minusMinutes(30);
        int i = 0;
        for (Problem p : List.of(p1, p2, pGroup, pOwn, pContest))
            solve(solver, p, base.plusMinutes(i++), SolveResult.SolveResultState.CORRECT);
    }

    static long countNotifications(User u, String type) {
        return jdbc.queryForObject("SELECT count(*) FROM notification WHERE user_id = ? AND type = ?", Long.class, u.getId(), type);
    }

    @Test @Order(1)
    void seededStoreAndCursor() {
        assertEquals(8, (int) jdbc.queryForObject("SELECT count(*) FROM store_item", Integer.class));
        assertNotNull(jdbc.queryForObject("SELECT last_id FROM notification_cursor WHERE name = 'solve_result'", Long.class));
    }

    @Test @Order(2)
    void reconcileFollowsScoreRuleAndIsIdempotent() {
        tx.executeWithoutResult(s -> points.reconcile(solver.getId()));
        tx.executeWithoutResult(s -> points.reconcile(solver.getId()));
        // p1(10) + p2(30). 그룹·대회 문제와 본인 문제는 제외
        assertEquals(40, points.balance(solver.getId()));
        assertEquals(40, points.earnedToday(solver.getId()));
        assertEquals(2, (int) jdbc.queryForObject("SELECT count(*) FROM point_ledger WHERE user_id = ?", Integer.class, solver.getId()));
    }

    @Test @Order(3)
    void purchaseEquipAndLookup() {
        long hunter = jdbc.queryForObject("SELECT id FROM store_item WHERE asset_key = 'title-binary-hunter'", Long.class);
        long frame = jdbc.queryForObject("SELECT id FROM store_item WHERE asset_key = 'frame-default'", Long.class);
        var poor = assertThrows(DomainRuleException.class, () -> tx.executeWithoutResult(s -> store.purchase(solver.getId(), hunter)));
        assertEquals(HttpStatus.BAD_REQUEST, poor.getStatus());

        // 지난 3일 동안 하루에 레벨 7 문제 두 개(140P)씩 풀었다 → 하루 한도 100P씩만 지급(70 + 30)
        for (int day = 1; day <= 3; day++) {
            LocalDateTime at = LocalDateTime.now().minusDays(day).withHour(10).withMinute(0).withSecond(0).withNano(0);
            Problem[] hard = new Problem[2];
            tx.executeWithoutResult(s -> { hard[0] = problem(author, "7", "public"); hard[1] = problem(author, "7", "public"); });
            solve(solver, hard[0], at, SolveResult.SolveResultState.CORRECT);
            solve(solver, hard[1], at.plusMinutes(5), SolveResult.SolveResultState.CORRECT);
            if (day == 1) yesterdayHard = hard[0];
        }
        int left = tx.execute(s -> store.purchase(solver.getId(), hunter));
        assertEquals(40, left); // 40 + 300 - 300
        var capped = jdbc.queryForList("SELECT amount FROM point_ledger WHERE user_id = ? AND earned_on = ? ORDER BY amount DESC",
                Integer.class, solver.getId(), java.sql.Date.valueOf(java.time.LocalDate.now().minusDays(1)));
        assertEquals(List.of(70, 30), capped);
        var dup = assertThrows(DomainRuleException.class, () -> tx.executeWithoutResult(s -> store.purchase(solver.getId(), hunter)));
        assertEquals(HttpStatus.CONFLICT, dup.getStatus());
        assertEquals(40, (int) tx.execute(s -> store.purchase(solver.getId(), frame)));

        tx.executeWithoutResult(s -> { store.equip(solver.getId(), hunter); store.equip(solver.getId(), frame); });
        var eq = store.equipped(List.of(solver.getId(), author.getId())).get(0);
        assertEquals("title-binary-hunter", eq.getTitle());
        assertEquals("Binary Hunter", eq.getTitleName());
        assertEquals("frame-default", eq.getAvatar());
        tx.executeWithoutResult(s -> store.unequip(solver.getId(), "title"));
        assertNull(store.equipped(List.of(solver.getId())).get(0).getTitle());

        var items = store.items(solver.getId());
        assertEquals(8, items.size());
        assertTrue(items.stream().anyMatch(i -> i.getItemId() == hunter && i.isOwned() && !i.isEquipped()));
        assertTrue(store.items(null).stream().noneMatch(i -> i.isOwned()));
        var me = tx.execute(s -> store.me(solver.getId()));
        @SuppressWarnings("unchecked") var history = (List<Object>) me.get("history");
        assertEquals(9, history.size()); // SOLVE 8줄 + 구매 1줄(0P 구매는 내역에서 뺀다)
        assertEquals(40, me.get("balance"));
        assertEquals(100, me.get("dailyCap"));
    }

    @Test @Order(4)
    void dailyProblemBonusRespectsTheCap() {
        var today = new net.diveon.backend.domain.problem.service.ProblemTodayService(
                problems, submissions, jdbc) {
            @Override protected java.time.LocalDate today() { return java.time.LocalDate.now(); }
        };
        jdbc.update("INSERT INTO daily_problem (day, prob_id) VALUES (?, ?)", java.sql.Date.valueOf(java.time.LocalDate.now()), p1.getId());
        jdbc.update("INSERT INTO daily_problem (day, prob_id) VALUES (?, ?)", java.sql.Date.valueOf(java.time.LocalDate.now().minusDays(1)), yesterdayHard.getId());
        assertEquals(p1.getId(), (long) tx.execute(s -> today.todayProblemId()));

        tx.executeWithoutResult(s -> points.reconcile(solver.getId()));
        tx.executeWithoutResult(s -> points.reconcile(solver.getId()));
        // 오늘: 40 + 오늘의 문제 50 = 90. 어제는 이미 100P라 오늘의 문제 보너스가 0P
        assertEquals(90, points.earnedToday(solver.getId()));
        assertEquals(90, points.balance(solver.getId()));
        assertEquals(List.of(50, 0), jdbc.queryForList(
                "SELECT amount FROM point_ledger WHERE user_id = ? AND reason = 'DAILY' ORDER BY earned_on DESC", Integer.class, solver.getId()));

        // 기록이 없는 날은 공개 문제 하나를 골라 기록하고, 다시 물어도 같은 문제다.
        var future = new net.diveon.backend.domain.problem.service.ProblemTodayService(problems, submissions, jdbc) {
            @Override protected java.time.LocalDate today() { return java.time.LocalDate.now().plusDays(1); }
        };
        long picked = tx.execute(s -> future.todayProblemId());
        assertEquals(picked, (long) tx.execute(s -> future.todayProblemId()));
        assertEquals("public", jdbc.queryForObject("SELECT visibility FROM problem_summary WHERE id = ?", String.class, picked));
    }

    @Test @Order(5)
    void solveNotificationsUseCursorAndSettings() {
        jdbc.update("UPDATE notification_cursor SET last_id = (SELECT COALESCE(MAX(id), 0) FROM solve_result) WHERE name = 'solve_result'");
        tx.executeWithoutResult(s -> notifications.updateSettings(other.getId(), new NotificationSettings(false, true, false)));
        tx.executeWithoutResult(s -> {
            var a = new SolveSubmission(p1, solver); em.persist(a); em.persist(new SolveResult(a, SolveResult.SolveResultState.CORRECT));
            var b = new SolveSubmission(p2, solver); em.persist(b); em.persist(new SolveResult(b, SolveResult.SolveResultState.WRONG));
            var c = new SolveSubmission(p1, other); em.persist(c); em.persist(new SolveResult(c, SolveResult.SolveResultState.CORRECT));
        });
        assertEquals(1, (int) tx.execute(s -> notifications.processSolveResults(500)));
        assertEquals(0, (int) tx.execute(s -> notifications.processSolveResults(500)));
        assertEquals(1, countNotifications(solver, NotificationService.SOLVE_CORRECT));
        assertEquals(0, countNotifications(other, NotificationService.SOLVE_CORRECT));
        String body = jdbc.queryForObject("SELECT body FROM notification WHERE user_id = ?", String.class, solver.getId());
        assertTrue(body.startsWith("#" + p1.getId() + " "), body);
    }

    @Test @Order(6)
    void commentNotificationsSkipSelfAndRespectSettings() {
        notifications.problemComment(author.getId(), solver.getId(), "solver", 900, p1.getId(), "t", false);
        notifications.problemComment(author.getId(), author.getId(), "author", 901, p1.getId(), "t", false);
        notifications.problemComment(author.getId(), solver.getId(), "solver", 900, p1.getId(), "t", false);
        assertEquals(1, countNotifications(author, NotificationService.COMMENT));
        tx.executeWithoutResult(s -> notifications.updateSettings(author.getId(), new NotificationSettings(null, false, null)));
        notifications.problemComment(author.getId(), solver.getId(), "solver", 902, p1.getId(), "t", false);
        assertEquals(1, countNotifications(author, NotificationService.COMMENT));
        var settings = notifications.settings(author.getId());
        assertTrue(settings.getSolveCorrect());
        assertFalse(settings.getComment());
    }

    @Test @Order(7)
    void contestStartOnlyWithinAnHourOnce() {
        tx.executeWithoutResult(s -> {
            var soon = new Contest(author, null, "곧 시작", "d", Contest.ContestType.INDIVIDUAL, Contest.ParticipationType.INDIVIDUAL,
                    Contest.Visibility.PUBLIC, LocalDateTime.now().plusMinutes(30), LocalDateTime.now().plusHours(2), null, null, null);
            var later = new Contest(author, null, "나중", "d", Contest.ContestType.INDIVIDUAL, Contest.ParticipationType.INDIVIDUAL,
                    Contest.Visibility.PUBLIC, LocalDateTime.now().plusHours(3), LocalDateTime.now().plusHours(4), null, null, null);
            em.persist(soon); em.persist(later);
            em.persist(new ContestParticipant(soon, solver, ContestParticipant.ContestRole.PARTICIPANT));
            em.persist(new ContestParticipant(soon, other, ContestParticipant.ContestRole.PARTICIPANT));
            em.persist(new ContestParticipant(later, solver, ContestParticipant.ContestRole.PARTICIPANT));
        });
        tx.executeWithoutResult(s -> notifications.updateSettings(solver.getId(), new NotificationSettings(null, null, true)));
        assertEquals(1, (int) tx.execute(s -> notifications.notifyContestStarts()));
        assertEquals(0, (int) tx.execute(s -> notifications.notifyContestStarts()));
        assertEquals(1, countNotifications(solver, NotificationService.CONTEST_START));
        assertEquals(0, countNotifications(other, NotificationService.CONTEST_START));

        var page = notifications.list(solver.getId(), 1, 20);
        assertEquals(2, page.getTotal());
        assertEquals(2, page.getUnreadCount());
        tx.executeWithoutResult(s -> notifications.markRead(solver.getId(), page.getNotifications().get(0).getId()));
        assertEquals(1, notifications.unreadCount(solver.getId()));
        assertThrows(DomainRuleException.class, () -> tx.executeWithoutResult(s -> notifications.markRead(other.getId(), page.getNotifications().get(1).getId())));
        tx.executeWithoutResult(s -> notifications.markAllRead(solver.getId()));
        assertEquals(0, notifications.unreadCount(solver.getId()));
    }

    @Test @Order(8)
    void withdrawalBlocksLeadersAndAnonymizesMembers() {
        var images = mock(ImageUploadService.class);
        var userService = mock(UserService.class);
        var withdrawn = mock(WithdrawnUsers.class);
        var encoder = mock(PasswordEncoder.class);
        when(encoder.encode(anyString())).thenReturn("encoded");
        var groupService = mock(GroupService.class);
        var service = new UserWithdrawService(users, groupUsers, groupService, notifications, store, images,
                encoder, userService, withdrawn, jdbc, 86400);

        long[] ids = new long[3];
        tx.executeWithoutResult(s -> {
            var leader = user("leader");
            var member = user("member");
            var solo = user("solo");
            var shared = new Group(leader, null, null, "공유 그룹", "d", false, true, null, null);
            var alone = new Group(solo, null, null, "혼자 그룹", "d", false, true, null, null);
            em.persist(shared); em.persist(alone);
            em.persist(new GroupUser(shared, leader, GroupUser.GroupRole.LEADER));
            em.persist(new GroupUser(shared, member, GroupUser.GroupRole.MEMBER));
            em.persist(new GroupUser(alone, solo, GroupUser.GroupRole.LEADER));
            ids[0] = leader.getId(); ids[1] = member.getId(); ids[2] = solo.getId();
        });

        var blocked = assertThrows(DomainRuleException.class, () -> tx.executeWithoutResult(s -> service.withdraw(ids[0], "leader")));
        assertEquals(HttpStatus.CONFLICT, blocked.getStatus());
        assertTrue(blocked.getMessage().contains("공유 그룹"));
        var hosted = assertThrows(DomainRuleException.class, () -> tx.executeWithoutResult(s -> service.withdraw(author.getId(), "author")));
        assertTrue(hosted.getMessage().contains("곧 시작"));
        var wrong = assertThrows(DomainRuleException.class, () -> tx.executeWithoutResult(s -> service.withdraw(ids[1], "someone")));
        assertEquals(HttpStatus.BAD_REQUEST, wrong.getStatus());

        tx.executeWithoutResult(s -> service.withdraw(ids[1], "member"));
        var row = jdbc.queryForMap("SELECT nickname, email, login_id, deleted_at FROM domain_user WHERE id = ?", ids[1]);
        assertEquals("탈퇴한 사용자#" + ids[1], row.get("nickname"));
        assertNull(row.get("email"));
        assertTrue(String.valueOf(row.get("login_id")).startsWith("deleted_" + ids[1] + "_"));
        assertNotNull(row.get("deleted_at"));
        assertEquals(0, (int) jdbc.queryForObject("SELECT count(*) FROM group_user WHERE user_id = ?", Integer.class, ids[1]));
        verify(images).delete("profiles/users/" + ids[1]);
        verify(userService).logout(String.valueOf(ids[1]));
        verify(withdrawn).mark(eq(ids[1]), any());

        tx.executeWithoutResult(s -> service.withdraw(ids[2], "solo"));
        verify(groupService).deleteGroup(anyLong(), eq(ids[2]));
        var again = assertThrows(DomainRuleException.class, () -> tx.executeWithoutResult(s -> service.withdraw(ids[1], "탈퇴한 사용자#" + ids[1])));
        assertEquals(HttpStatus.NOT_FOUND, again.getStatus());
    }

    @Test @Order(9)
    void activityShowsPublicResultsOfActiveUsersOnly() {
        var before = activity.getRecent(20).getActivities();
        assertTrue(before.stream().anyMatch(a -> a.getUserId().equals(other.getId())));
        jdbc.update("UPDATE domain_user SET deleted_at = now() WHERE id = ?", other.getId());
        var after = activity.getRecent(20).getActivities();
        assertTrue(after.stream().noneMatch(a -> a.getUserId().equals(other.getId())));
        assertTrue(after.stream().allMatch(a -> a.getSubmittedAt() != null));
        assertEquals(0, store.equipped(List.of(other.getId())).size());
    }
}

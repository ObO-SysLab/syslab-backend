package net.diveon.backend.domain.notification.service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import net.diveon.backend.domain.notification.dto.NotificationListResponse;
import net.diveon.backend.domain.notification.dto.NotificationSettings;
import net.diveon.backend.global.exception.DomainRuleException;

// 알림 저장·조회·설정. 테이블은 ops/2026-11-12-account-store-notification.sql.
@Service
public class NotificationService implements CommentNotifier {

    public static final String SOLVE_CORRECT = "SOLVE_CORRECT";
    public static final String COMMENT = "COMMENT";
    public static final String REPLY = "REPLY";
    public static final String GROUP_COMMENT = "GROUP_COMMENT";
    public static final String CONTEST_START = "CONTEST_START";

    static final int MAX_BODY = 300;
    static final int MAX_PAGE_SIZE = 50;

    // 댓글 알림: 받는 사람이 활성 계정이고 댓글 알림을 끄지 않았을 때만 넣는다.
    static final String INSERT_COMMENT = """
            INSERT INTO notification (user_id, type, title, body, link, ref_id, is_read, created_at)
            SELECT u.id, ?, ?, ?, ?, ?, false, ?
            FROM domain_user u
            LEFT JOIN notification_setting ns ON ns.user_id = u.id
            WHERE u.id = ? AND u.deleted_at IS NULL AND COALESCE(ns.comment_reply, true)
            ON CONFLICT (user_id, type, ref_id) DO NOTHING
            """;

    // 정답 알림: 처리 위치 다음의 채점 결과 중 CORRECT만. 코딩(외부 채점기)·객관식·실습 결과를 모두 덮는다.
    static final String INSERT_SOLVE = """
            INSERT INTO notification (user_id, type, title, body, link, ref_id, is_read, created_at)
            SELECT ss.submitter_id, 'SOLVE_CORRECT', '정답',
                   LEFT('#' || ps.id || ' ' || ps.title || ' 문제를 맞혔어요.', 300),
                   '/challenges/detail?id=' || ps.id, sr.id, false, ?
            FROM solve_result sr
            JOIN solve_submission ss ON ss.id = sr.submission_id
            JOIN problem_summary ps ON ps.id = ss.prob_id
            JOIN domain_user u ON u.id = ss.submitter_id AND u.deleted_at IS NULL
            LEFT JOIN notification_setting ns ON ns.user_id = ss.submitter_id
            WHERE sr.id > ? AND sr.id <= ? AND sr.result_status = 'CORRECT' AND COALESCE(ns.solve_correct, true)
            ON CONFLICT (user_id, type, ref_id) DO NOTHING
            """;

    // 대회 시작 1시간 전 알림: 참가자 중 설정을 켠 사람. 유니크 제약으로 대회마다 한 번만 간다.
    static final String INSERT_CONTEST_START = """
            INSERT INTO notification (user_id, type, title, body, link, ref_id, is_read, created_at)
            SELECT cp.user_id, 'CONTEST_START', '대회 시작 1시간 전',
                   LEFT(c.title || ' 대회가 ' || to_char(c.start_time, 'HH24:MI') || '에 시작해요.', 300),
                   '/contests/detail?id=' || c.id, c.id, false, ?
            FROM contest c
            JOIN contest_participant cp ON cp.contest_id = c.id AND cp.is_banned = false
            JOIN domain_user u ON u.id = cp.user_id AND u.deleted_at IS NULL
            LEFT JOIN notification_setting ns ON ns.user_id = cp.user_id
            WHERE c.start_time > ? AND c.start_time <= ? AND COALESCE(ns.contest_start, false)
            ON CONFLICT (user_id, type, ref_id) DO NOTHING
            """;

    static final String LOCK_CURSOR = "SELECT last_id FROM notification_cursor WHERE name = 'solve_result' FOR UPDATE";
    static final String NEXT_MAX = "SELECT MAX(id) FROM (SELECT id FROM solve_result WHERE id > ? ORDER BY id LIMIT ?) b";
    static final String MOVE_CURSOR = "UPDATE notification_cursor SET last_id = ? WHERE name = 'solve_result'";

    private final JdbcTemplate jdbc;

    public NotificationService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    protected LocalDateTime now() {
        return LocalDateTime.now();
    }

    // ---------- 댓글 알림 (댓글 저장과 별도 트랜잭션: 알림이 실패해도 댓글은 남는다) ----------

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void problemComment(long recipientId, long actorId, String actorNickname,
                               long commentId, long probId, String problemTitle, boolean reply) {
        if (recipientId == actorId) return;
        String body = reply
                ? actorNickname + "님이 내 댓글에 답글을 남겼어요. #" + probId + " " + problemTitle
                : actorNickname + "님이 #" + probId + " " + problemTitle + "에 댓글을 남겼어요.";
        insertComment(recipientId, reply ? REPLY : COMMENT, reply ? "새 답글" : "새 댓글", body,
                "/challenges/detail?id=" + probId + "&tab=comments", commentId);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void groupPostComment(long recipientId, long actorId, String actorNickname,
                                 long commentId, long groupId, String postTitle) {
        if (recipientId == actorId) return;
        insertComment(recipientId, GROUP_COMMENT, "새 댓글",
                actorNickname + "님이 '" + postTitle + "' 글에 댓글을 남겼어요.",
                "/groups/detail?id=" + groupId, commentId);
    }

    private void insertComment(long recipientId, String type, String title, String body, String link, long refId) {
        jdbc.update(INSERT_COMMENT, type, title, truncate(body), link, refId, Timestamp.valueOf(now()), recipientId);
    }

    static String truncate(String body) {
        return body.length() <= MAX_BODY ? body : body.substring(0, MAX_BODY - 1) + "…";
    }

    // ---------- 예약 작업 ----------

    // 처리 위치 다음의 채점 결과를 최대 batch개 읽어 정답 알림을 넣고 위치를 옮긴다. 위치 행을 잠가 서버가 여러 대여도 한 번만 처리한다.
    @Transactional
    public int processSolveResults(int batch) {
        List<Long> cursor = jdbc.query(LOCK_CURSOR, (rs, i) -> rs.getLong(1));
        if (cursor.isEmpty()) return 0;
        long last = cursor.get(0);
        Long max = jdbc.queryForObject(NEXT_MAX, Long.class, last, batch);
        if (max == null) return 0;
        int inserted = jdbc.update(INSERT_SOLVE, Timestamp.valueOf(now()), last, max);
        jdbc.update(MOVE_CURSOR, max);
        return inserted;
    }

    @Transactional
    public int notifyContestStarts() {
        LocalDateTime now = now();
        return jdbc.update(INSERT_CONTEST_START, Timestamp.valueOf(now), Timestamp.valueOf(now),
                Timestamp.valueOf(now.plusHours(1)));
    }

    // ---------- 조회 ----------

    @Transactional(readOnly = true)
    public NotificationListResponse list(long userId, int page, int size) {
        int p = Math.max(page, 1);
        int s = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Long total = jdbc.queryForObject("SELECT count(*) FROM notification WHERE user_id = ?", Long.class, userId);
        List<NotificationListResponse.Item> items = jdbc.query("""
                SELECT id, type, title, body, link, is_read, created_at FROM notification
                WHERE user_id = ? ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?
                """, (rs, i) -> new NotificationListResponse.Item(
                rs.getLong("id"), rs.getString("type"), rs.getString("title"), rs.getString("body"),
                rs.getString("link"), rs.getBoolean("is_read"), rs.getTimestamp("created_at").toLocalDateTime()),
                userId, s, (p - 1) * s);
        return new NotificationListResponse(total == null ? 0 : total, unreadCount(userId), p, s, items);
    }

    @Transactional(readOnly = true)
    public long unreadCount(long userId) {
        Long count = jdbc.queryForObject("SELECT count(*) FROM notification WHERE user_id = ? AND is_read = false",
                Long.class, userId);
        return count == null ? 0 : count;
    }

    @Transactional
    public void markRead(long userId, long notificationId) {
        int updated = jdbc.update("UPDATE notification SET is_read = true WHERE id = ? AND user_id = ?",
                notificationId, userId);
        if (updated == 0) throw new DomainRuleException(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다.");
    }

    @Transactional
    public int markAllRead(long userId) {
        return jdbc.update("UPDATE notification SET is_read = true WHERE user_id = ? AND is_read = false", userId);
    }

    // ---------- 설정 ----------

    @Transactional(readOnly = true)
    public NotificationSettings settings(long userId) {
        List<NotificationSettings> rows = jdbc.query(
                "SELECT solve_correct, comment_reply, contest_start FROM notification_setting WHERE user_id = ?",
                (rs, i) -> new NotificationSettings(rs.getBoolean(1), rs.getBoolean(2), rs.getBoolean(3)), userId);
        return rows.isEmpty() ? NotificationSettings.defaults() : rows.get(0);
    }

    // 보낸 값만 바꾸고 나머지는 현재 값을 유지한다.
    @Transactional
    public NotificationSettings updateSettings(long userId, NotificationSettings request) {
        NotificationSettings current = settings(userId);
        NotificationSettings next = new NotificationSettings(
                request.getSolveCorrect() != null ? request.getSolveCorrect() : current.getSolveCorrect(),
                request.getComment() != null ? request.getComment() : current.getComment(),
                request.getContestStart() != null ? request.getContestStart() : current.getContestStart());
        jdbc.update("""
                INSERT INTO notification_setting (user_id, solve_correct, comment_reply, contest_start)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (user_id) DO UPDATE SET solve_correct = EXCLUDED.solve_correct,
                    comment_reply = EXCLUDED.comment_reply, contest_start = EXCLUDED.contest_start
                """, userId, next.getSolveCorrect(), next.getComment(), next.getContestStart());
        return next;
    }

    // 계정 탈퇴: 받은 알림과 설정을 지운다(탈퇴 트랜잭션 안에서 호출).
    public void deleteAllFor(long userId) {
        jdbc.update("DELETE FROM notification WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM notification_setting WHERE user_id = ?", userId);
    }
}

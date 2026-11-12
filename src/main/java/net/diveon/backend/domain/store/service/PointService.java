package net.diveon.backend.domain.store.service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.diveon.backend.domain.store.dto.PointHistoryItem;

// 포인트 장부(point_ledger). 잔액 = amount 합.
@Service
public class PointService {

    // 풀이 기록(problem_solver)에서 아직 장부에 없는 문제를 SOLVE 행으로 옮긴다. 여러 번 실행해도 결과가 같다.
    // 점수 규칙(SolveResultRepository.calculateTierScoreByUserId)과 같게: 난이도×10점, 그룹·대회 문제와 본인이 낸 문제는 제외.
    // problem_solver는 코딩(외부 채점기)·객관식·실습·대회 정답을 모두 기록하므로 채점 경로마다 지급 코드를 둘 필요가 없다.
    static final String RECONCILE_SQL = """
            INSERT INTO point_ledger (user_id, amount, reason, ref_id, created_at)
            SELECT s.user_id, CAST(ps.difficulty AS integer) * 10, 'SOLVE', ps.id, ?
            FROM problem_solver s
            JOIN problem_summary ps ON ps.id = s.prob_id
            WHERE s.user_id = ?
              AND ps.difficulty IN ('1', '2', '3', '4', '5', '6', '7')
              AND ps.visibility NOT IN ('group', 'contest')
              AND ps.author_id <> s.user_id
            ON CONFLICT (user_id, reason, ref_id) DO NOTHING
            """;

    static final String BALANCE_SQL = "SELECT COALESCE(SUM(amount), 0) FROM point_ledger WHERE user_id = ?";

    static final String HISTORY_SQL = """
            SELECT l.amount, l.reason, l.ref_id, l.created_at,
                   CASE WHEN l.reason = 'SOLVE' THEN ps.title ELSE si.name END AS label
            FROM point_ledger l
            LEFT JOIN problem_summary ps ON l.reason = 'SOLVE' AND ps.id = l.ref_id
            LEFT JOIN store_item si ON l.reason = 'PURCHASE' AND si.id = l.ref_id
            WHERE l.user_id = ?
            ORDER BY l.created_at DESC, l.id DESC
            LIMIT ?
            """;

    private final JdbcTemplate jdbc;

    public PointService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    protected LocalDateTime now() {
        return LocalDateTime.now();
    }

    @Transactional
    public int reconcile(long userId) {
        return jdbc.update(RECONCILE_SQL, Timestamp.valueOf(now()), userId);
    }

    public int balance(long userId) {
        Integer sum = jdbc.queryForObject(BALANCE_SQL, Integer.class, userId);
        return sum == null ? 0 : sum;
    }

    public List<PointHistoryItem> history(long userId, int limit) {
        return jdbc.query(HISTORY_SQL, (rs, i) -> new PointHistoryItem(
                rs.getInt("amount"), rs.getString("reason"), rs.getLong("ref_id"), rs.getString("label"),
                rs.getTimestamp("created_at").toLocalDateTime()), userId, limit);
    }

    public void spend(long userId, long itemId, int price) {
        jdbc.update("""
                INSERT INTO point_ledger (user_id, amount, reason, ref_id, created_at)
                VALUES (?, ?, 'PURCHASE', ?, ?)
                """, userId, -price, itemId, Timestamp.valueOf(now()));
    }
}

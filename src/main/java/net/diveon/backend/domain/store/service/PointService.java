package net.diveon.backend.domain.store.service;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.diveon.backend.domain.store.dto.PointHistoryItem;

// 포인트 장부(point_ledger). 잔액 = amount 합.
// 획득: 공개 문제 첫 정답 난이도×10P(SOLVE), 그날의 오늘의 다이브 포인트 정답 +50P(DAILY).
// 하루(서울 날짜) 획득 한도 100P. 넘는 만큼은 그날 지급하지 않는다. 최고가 상품 5,000P = 한도로 50일.
@Service
public class PointService {

    public static final int DAILY_CAP = 100;
    public static final int DAILY_BONUS = 50;

    // 아직 장부에 없는 획득 이벤트를 찾아 날짜별 한도 안에서 지급한다. 여러 번 실행해도 결과가 같다.
    // - SOLVE: 점수 규칙(SolveResultRepository.calculateTierScoreByUserId)과 같은 조건. 그룹·대회 문제와 본인이 낸 문제는 제외.
    //   시각은 첫 정답 제출의 submitted_at(백엔드가 모든 채점 경로에서 서울 시간으로 기록).
    // - DAILY: daily_problem의 그날 문제를 그날 맞힌 경우. 이미 푼 문제여도 그날 다시 맞히면 인정.
    // - 한도: 같은 날 먼저 생긴 이벤트부터 채운다. 남은 한도 = 100 - 이미 지급된 그날 합 - 같은 날 앞선 미지급 이벤트 합.
    static final String RECONCILE_SQL = """
            WITH first_solve AS (
                SELECT DISTINCT ON (ss.prob_id)
                       CAST('SOLVE' AS varchar) AS reason, ss.prob_id AS ref_id, ss.submitted_at AS at,
                       CAST(ps.difficulty AS integer) * 10 AS points
                FROM solve_submission ss
                JOIN solve_result sr ON sr.submission_id = ss.id AND sr.result_status = 'CORRECT'
                JOIN problem_summary ps ON ps.id = ss.prob_id
                WHERE ss.submitter_id = ?
                  AND ps.difficulty IN ('1', '2', '3', '4', '5', '6', '7')
                  AND ps.visibility NOT IN ('group', 'contest')
                  AND ps.author_id <> ss.submitter_id
                ORDER BY ss.prob_id, ss.submitted_at, ss.id
            ),
            daily AS (
                SELECT CAST('DAILY' AS varchar) AS reason,
                       CAST(to_char(dp.day, 'YYYYMMDD') AS bigint) AS ref_id,
                       MIN(ss.submitted_at) AS at,
                       ? AS points
                FROM daily_problem dp
                JOIN solve_submission ss ON ss.prob_id = dp.prob_id AND ss.submitter_id = ?
                     AND ss.submitted_at >= dp.day AND ss.submitted_at < dp.day + 1
                JOIN solve_result sr ON sr.submission_id = ss.id AND sr.result_status = 'CORRECT'
                GROUP BY dp.day
            ),
            events AS (
                SELECT reason, ref_id, at, CAST(at AS date) AS day, points FROM first_solve
                UNION ALL
                SELECT reason, ref_id, at, CAST(at AS date) AS day, points FROM daily
            ),
            pending AS (
                SELECT e.* FROM events e
                WHERE NOT EXISTS (SELECT 1 FROM point_ledger l
                                  WHERE l.user_id = ? AND l.reason = e.reason AND l.ref_id = e.ref_id)
            ),
            used AS (
                SELECT earned_on, SUM(amount) AS total FROM point_ledger
                WHERE user_id = ? AND reason IN ('SOLVE', 'DAILY') GROUP BY earned_on
            ),
            ordered AS (
                SELECT p.*, COALESCE(u.total, 0) AS used_before,
                       SUM(p.points) OVER (PARTITION BY p.day ORDER BY p.at, p.reason, p.ref_id
                                           ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) - p.points AS earlier
                FROM pending p LEFT JOIN used u ON u.earned_on = p.day
            )
            INSERT INTO point_ledger (user_id, amount, reason, ref_id, earned_on, created_at)
            SELECT ?, GREATEST(0, LEAST(points, ? - used_before - earlier)), reason, ref_id, day, ?
            FROM ordered
            ON CONFLICT (user_id, reason, ref_id) DO NOTHING
            """;

    static final String BALANCE_SQL = "SELECT COALESCE(SUM(amount), 0) FROM point_ledger WHERE user_id = ?";

    static final String TODAY_SQL = """
            SELECT COALESCE(SUM(amount), 0) FROM point_ledger
            WHERE user_id = ? AND reason IN ('SOLVE', 'DAILY') AND earned_on = ?
            """;

    static final String HISTORY_SQL = """
            SELECT l.amount, l.reason, l.ref_id, l.created_at,
                   CASE WHEN l.reason = 'SOLVE' THEN ps.title
                        WHEN l.reason = 'PURCHASE' THEN si.name
                        ELSE dps.title END AS label
            FROM point_ledger l
            LEFT JOIN problem_summary ps ON l.reason = 'SOLVE' AND ps.id = l.ref_id
            LEFT JOIN store_item si ON l.reason = 'PURCHASE' AND si.id = l.ref_id
            LEFT JOIN daily_problem dp ON l.reason = 'DAILY' AND l.earned_on = dp.day
            LEFT JOIN problem_summary dps ON dps.id = dp.prob_id
            WHERE l.user_id = ? AND l.amount <> 0
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
        return jdbc.update(RECONCILE_SQL, userId, DAILY_BONUS, userId, userId, userId, userId, DAILY_CAP,
                Timestamp.valueOf(now()));
    }

    public int balance(long userId) {
        Integer sum = jdbc.queryForObject(BALANCE_SQL, Integer.class, userId);
        return sum == null ? 0 : sum;
    }

    // 오늘(서울 날짜) 받은 포인트. 한도 표시용
    public int earnedToday(long userId) {
        Integer sum = jdbc.queryForObject(TODAY_SQL, Integer.class, userId, Date.valueOf(now().toLocalDate()));
        return sum == null ? 0 : sum;
    }

    public List<PointHistoryItem> history(long userId, int limit) {
        return jdbc.query(HISTORY_SQL, (rs, i) -> new PointHistoryItem(
                rs.getInt("amount"), rs.getString("reason"), rs.getLong("ref_id"), rs.getString("label"),
                rs.getTimestamp("created_at").toLocalDateTime()), userId, limit);
    }

    public void spend(long userId, long itemId, int price) {
        LocalDateTime at = now();
        jdbc.update("""
                INSERT INTO point_ledger (user_id, amount, reason, ref_id, earned_on, created_at)
                VALUES (?, ?, 'PURCHASE', ?, ?, ?)
                """, userId, -price, itemId, Date.valueOf(at.toLocalDate()), Timestamp.valueOf(at));
    }

}

package net.diveon.backend.domain.activity.service;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import net.diveon.backend.domain.activity.dto.RecentActivityResponse;

@Service
public class RecentActivityService {

    static final int DEFAULT_LIMIT = 10;
    static final int MAX_LIMIT = 20;

    // 시각은 solve_submission.submitted_at을 쓴다. solve_result.created_at은 코딩 채점기가 DB 시간(UTC)으로,
    // 나머지 경로는 백엔드가 서울 시간으로 기록해 서로 어긋날 수 있다. submitted_at은 모든 경로에서 백엔드가 기록한다.
    static final String RECENT_SQL = """
            SELECT sr.id AS result_id, sr.result_status, ss.submitted_at,
                   u.id AS user_id, u.nickname, ps.id AS prob_id, ps.title
            FROM solve_result sr
            JOIN solve_submission ss ON ss.id = sr.submission_id
            JOIN problem_summary ps ON ps.id = ss.prob_id
            JOIN domain_user u ON u.id = ss.submitter_id
            WHERE ps.visibility = 'public'
            ORDER BY sr.id DESC
            LIMIT ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public RecentActivityService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public RecentActivityResponse getRecent(Integer limit) {
        List<RecentActivityResponse.Item> items = jdbcTemplate.query(RECENT_SQL, (rs, rowNum) -> {
            Timestamp submittedAt = rs.getTimestamp("submitted_at");
            return new RecentActivityResponse.Item(
                    rs.getLong("result_id"),
                    rs.getString("result_status"),
                    submittedAt == null ? null : submittedAt.toLocalDateTime(),
                    rs.getLong("user_id"),
                    rs.getString("nickname"),
                    rs.getLong("prob_id"),
                    rs.getString("title"));
        }, clampLimit(limit));
        return new RecentActivityResponse(items);
    }

    static int clampLimit(Integer limit) {
        if (limit == null || limit < 1) return DEFAULT_LIMIT;
        return Math.min(limit, MAX_LIMIT);
    }
}

package net.diveon.backend.domain.user.service;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.diveon.backend.domain.grade.repository.SolveResultRepository;
import net.diveon.backend.domain.user.entity.User;
import net.diveon.backend.domain.user.repository.UserRepository;
import net.diveon.backend.global.exception.UserNotFoundException;

@Service
public class UserTierService {

    // SolveResultRepository.calculateTierScoreByUserId를 모든 활성 사용자에게 한 번에 적용한 것.
    // 공개·비공개 문제(그룹·대회 문제와 본인 문제 제외)의 문제별 점수(난이도×10) 중 높은 100개의 합.
    static final String ALL_SCORES = """
        WITH per_problem AS (
            SELECT ss.submitter_id AS user_id,
                   MAX(CASE ps.difficulty
                       WHEN '1' THEN 10 WHEN '2' THEN 20 WHEN '3' THEN 30 WHEN '4' THEN 40
                       WHEN '5' THEN 50 WHEN '6' THEN 60 WHEN '7' THEN 70 ELSE 0 END) AS score
            FROM solve_result sr
            JOIN solve_submission ss ON sr.submission_id = ss.id
            JOIN problem_summary ps ON ss.prob_id = ps.id
            WHERE sr.result_status = 'CORRECT'
              AND ps.visibility NOT IN ('group', 'contest')
              AND ps.author_id != ss.submitter_id
            GROUP BY ss.submitter_id, ps.id
        ), ranked AS (
            SELECT user_id, score, ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY score DESC) AS rn
            FROM per_problem
        ), totals AS (
            SELECT user_id, SUM(score) AS score FROM ranked WHERE rn <= 100 GROUP BY user_id
        )
        SELECT u.id, COALESCE(t.score, 0) AS calculated, u.score, u.tier
        FROM domain_user u
        LEFT JOIN totals t ON t.user_id = u.id
        WHERE u.deleted_at IS NULL
        """;

    private final UserRepository userRepository;
    private final SolveResultRepository solveResultRepository;
    private final JdbcTemplate jdbc;

    public UserTierService(UserRepository userRepository, SolveResultRepository solveResultRepository, JdbcTemplate jdbc) {
        this.userRepository = userRepository;
        this.solveResultRepository = solveResultRepository;
        this.jdbc = jdbc;
    }

    @Transactional
    public void refreshTierScore(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(UserNotFoundException::new);

        Integer score = solveResultRepository.calculateTierScoreByUserId(userId);
        int tierScore = score == null ? 0 : score;
        user.updateTierScore(tierScore, calculateTier(tierScore));
    }

    /**
     * 모든 활성 사용자의 점수·티어를 풀이 기록에서 다시 계산해, 저장된 값과 다른 사용자만 고친다.
     * 코딩 문제는 외부 채점기가 solve_result를 직접 넣어 refreshTierScore가 불리지 않는다. 이 경로를 주기적으로 맞춘다.
     * @return 고친 사용자 수
     */
    @Transactional
    public int refreshAllTierScores() {
        record Row(long id, int score, int tier) {}
        List<Row> changed = jdbc.query(ALL_SCORES, (rs, i) -> {
            int calculated = rs.getInt("calculated");
            int tier = calculateTier(calculated);
            return calculated == rs.getInt("score") && tier == rs.getInt("tier") ? null : new Row(rs.getLong("id"), calculated, tier);
        }).stream().filter(java.util.Objects::nonNull).toList();
        if (changed.isEmpty()) return 0;
        jdbc.batchUpdate("UPDATE domain_user SET score = ?, tier = ? WHERE id = ?",
                changed.stream().map(r -> new Object[] { r.score(), r.tier(), r.id() }).toList());
        return changed.size();
    }

    static int calculateTier(int score) {
        // 최고 티어를 7로 유지하므로 6,600점 이상을 별도 티어로 구분하지 않는다.
        // if (score >= 6600) return 8;
        if (score >= 5400) return 7;
        if (score >= 4000) return 6;
        if (score >= 2400) return 5;
        if (score >= 1350) return 4;
        if (score >= 600) return 3;
        if (score >= 150) return 2;
        return 1;
    }
}

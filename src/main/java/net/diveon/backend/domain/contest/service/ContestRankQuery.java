package net.diveon.backend.domain.contest.service;

/** Ordinal rank, matching the ranking list: score, earliest last solve, stable user ID. */
public final class ContestRankQuery {
    private ContestRankQuery() {}
    public static final String SQL = """
            SELECT position FROM (
                SELECT user_id, row_number() OVER (
                    ORDER BY score DESC, last_solved_at ASC NULLS LAST, user_id ASC
                ) AS position
                FROM contest_participant WHERE contest_id = ? AND is_banned = false
            ) ranked WHERE user_id = ?
            """;
}

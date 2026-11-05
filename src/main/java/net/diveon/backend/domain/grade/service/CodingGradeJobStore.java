package net.diveon.backend.domain.grade.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Durable requests. Always lock the common submission before its job (also used by the worker). */
@Repository
public class CodingGradeJobStore {
    private final JdbcTemplate jdbc;

    public CodingGradeJobStore(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public record Dispatch(long submissionId, int attempt, String payload) {}

    static final String REGISTER = """
        INSERT INTO coding_grade_job (submission_id, payload, timeout_seconds)
        VALUES (?, ?, ?) ON CONFLICT (submission_id) DO NOTHING
        """;
    static final String NEXT = """
        SELECT s.id FROM solve_submission s JOIN coding_grade_job j ON j.submission_id = s.id
        WHERE j.finished_at IS NULL AND j.deadline_at <= clock_timestamp()
        ORDER BY j.deadline_at, s.id LIMIT 1 FOR UPDATE OF s SKIP LOCKED
        """;
    static final String LOCK_JOB = """
        SELECT attempt_no, payload FROM coding_grade_job
        WHERE submission_id = ? AND finished_at IS NULL AND deadline_at <= clock_timestamp()
        FOR UPDATE
        """;
    static final String CLAIM = """
        UPDATE coding_grade_job SET attempt_no = attempt_no + 1,
            deadline_at = clock_timestamp() + timeout_seconds * interval '1 second',
            failure_code = NULL WHERE submission_id = ?
        """;
    static final String FINISH = """
        UPDATE coding_grade_job SET finished_at = clock_timestamp(), failure_code = ?
        WHERE submission_id = ?
        """;
    static final String FAIL_COMMON = """
        UPDATE solve_submission SET submission_state = 'FAILED' WHERE id = ?
        """;
    static final String FAIL_CONTEST = """
        UPDATE contest_submission SET submission_status = 'FAILED'
        WHERE submission_id = ? AND submission_status <> 'COMPLETED'
        """;
    static final String SEND_FAILED = """
        UPDATE coding_grade_job SET deadline_at = clock_timestamp(), failure_code = 'DISPATCH_FAILED'
        WHERE submission_id = ? AND attempt_no = ? AND finished_at IS NULL
        """;

    @Transactional
    public void register(long id, String payload, long timeoutSeconds) {
        if (timeoutSeconds <= 0) throw new IllegalArgumentException("Invalid grading timeout");
        jdbc.update(REGISTER, id, payload, timeoutSeconds);
    }

    @Transactional
    public Dispatch claimNext(int maxAttempts) {
        if (maxAttempts < 1) throw new IllegalArgumentException("Invalid attempt limit");
        var ids = jdbc.queryForList(NEXT, Long.class);
        if (ids.isEmpty()) return null;
        long id = ids.getFirst();
        var jobs = jdbc.query(LOCK_JOB,
                (rs, n) -> new Dispatch(id, rs.getInt("attempt_no"), rs.getString("payload")), id);
        if (jobs.isEmpty()) return new Dispatch(id, 0, null);
        Dispatch job = jobs.getFirst();
        String state = jdbc.queryForObject(
                "SELECT submission_state FROM solve_submission WHERE id = ?", String.class, id);
        if ("COMPLETED".equals(state) || "FAILED".equals(state)) {
            jdbc.update(FINISH, null, id);
            return new Dispatch(id, job.attempt(), null);
        }
        if (job.attempt() >= maxAttempts) {
            jdbc.update(FAIL_COMMON, id);
            jdbc.update(FAIL_CONTEST, id);
            jdbc.update(FINISH, "ATTEMPTS_EXHAUSTED", id);
            return new Dispatch(id, job.attempt(), null);
        }
        jdbc.update(CLAIM, id);
        return new Dispatch(id, job.attempt() + 1, job.payload());
    }

    @Transactional
    public void dispatchFailed(Dispatch job) {
        // No result/state changes; a lost network response may still have launched the task.
        jdbc.update(SEND_FAILED, job.submissionId(), job.attempt());
    }
}

-- AUDIT-10: prepared only. Pause all result writers and back up before approved execution.
BEGIN;
SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '60s';
CREATE TABLE IF NOT EXISTS problem_solver (
    prob_id bigint NOT NULL REFERENCES problem_summary(id) ON DELETE CASCADE,
    user_id bigint NOT NULL REFERENCES domain_user(id) ON DELETE CASCADE,
    PRIMARY KEY (prob_id, user_id)
);
-- Seed deduplication from both histories. Do NOT silently rewrite historical counters.
INSERT INTO problem_solver(prob_id, user_id)
SELECT s.prob_id, s.submitter_id FROM solve_submission s
JOIN solve_result r ON r.submission_id = s.id
WHERE r.result_status = 'CORRECT'
UNION
SELECT p.problem_id, s.user_id FROM contest_submission s
JOIN contest_problem p ON p.id = s.contest_problem_id
WHERE s.is_correct = true
ON CONFLICT (prob_id, user_id) DO NOTHING;
COMMIT;

-- Read-only discrepancy report. Any correction requires a separate reviewed data plan.
SELECT p.id, p.solved_count, count(s.user_id) AS distinct_solvers
FROM problem_summary p LEFT JOIN problem_solver s ON s.prob_id = p.id
GROUP BY p.id, p.solved_count HAVING p.solved_count <> count(s.user_id);

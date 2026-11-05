-- MANUAL migration only. Back up and inspect existing constraints before production approval.
-- Not placed in Spring's auto-executed migration directory.
BEGIN;
SET LOCAL lock_timeout = '5s';

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint c JOIN pg_attribute a
          ON a.attrelid = c.conrelid AND a.attnum = ANY(c.conkey)
        WHERE c.conrelid = 'solve_submission'::regclass AND c.contype = 'c'
          AND a.attname = 'submission_state'
          AND c.conname <> 'solve_submission_submission_state_check'
    ) THEN
        RAISE EXCEPTION 'Unknown submission_state check constraint; review before migration';
    END IF;
END $$;

-- Standard Hibernate constraint name. Abort/review if production uses another state constraint.
ALTER TABLE solve_submission DROP CONSTRAINT IF EXISTS solve_submission_submission_state_check;
ALTER TABLE solve_submission ADD CONSTRAINT solve_submission_submission_state_check
    CHECK (submission_state IN ('PENDING', 'JUDGING', 'COMPLETED', 'FAILED'));

CREATE TABLE coding_grade_job (
    submission_id bigint PRIMARY KEY REFERENCES solve_submission(id) ON DELETE CASCADE,
    payload text NOT NULL,
    attempt_no integer NOT NULL DEFAULT 0 CHECK (attempt_no BETWEEN 0 AND 3),
    timeout_seconds bigint NOT NULL CHECK (timeout_seconds > 0),
    deadline_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    finished_at timestamptz,
    failure_code varchar(64)
);
CREATE INDEX coding_grade_job_due ON coding_grade_job(deadline_at) WHERE finished_at IS NULL;
COMMIT;

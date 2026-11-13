-- Practice command restriction: PREPARED ONLY. Run with approval before deploying the backend that
-- maps this column (production uses ddl-auto: validate). Run with ON_ERROR_STOP.
-- Adds nullable columns only; existing rows stay NULL (= unrestricted, no failure detail) and keep working.
-- The legacy allowed_commands column is left untouched: it holds hidden form defaults, never a restriction.
BEGIN;
SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '30s';

ALTER TABLE problem_practice ADD COLUMN IF NOT EXISTS command_allowlist jsonb;
-- Validated author-facing build failure reason ("CODE" or "CODE:command").
ALTER TABLE problem_practice ADD COLUMN IF NOT EXISTS build_failure_detail varchar(80);

COMMIT;

-- Verification (read-only): both columns exist and no existing row is restricted.
-- SELECT column_name, data_type, is_nullable FROM information_schema.columns
--  WHERE table_name = 'problem_practice' AND column_name IN ('command_allowlist', 'build_failure_detail');
-- SELECT count(*) AS restricted FROM problem_practice WHERE command_allowlist IS NOT NULL;

-- Rollback (only after the backend that maps the columns has been rolled back):
-- ALTER TABLE problem_practice DROP COLUMN IF EXISTS build_failure_detail;
-- ALTER TABLE problem_practice DROP COLUMN IF EXISTS command_allowlist;

-- Legacy practice columns, step 1 of 2 (run BEFORE deploying the backend that no longer maps them).
-- os_image, cpu_limit and memory_limit are NOT NULL; the new backend stops writing them, so inserts
-- would fail until the constraint is lifted. Values are kept; step 2 drops the columns afterwards.
BEGIN;
SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '30s';

ALTER TABLE problem_practice ALTER COLUMN os_image DROP NOT NULL;
ALTER TABLE problem_practice ALTER COLUMN cpu_limit DROP NOT NULL;
ALTER TABLE problem_practice ALTER COLUMN memory_limit DROP NOT NULL;

COMMIT;

-- Verification (read-only): all three are nullable.
-- SELECT column_name, is_nullable FROM information_schema.columns
--  WHERE table_name = 'problem_practice' AND column_name IN ('os_image', 'cpu_limit', 'memory_limit');

-- Rollback (only while every row still has values):
-- ALTER TABLE problem_practice ALTER COLUMN os_image SET NOT NULL;
-- ALTER TABLE problem_practice ALTER COLUMN cpu_limit SET NOT NULL;
-- ALTER TABLE problem_practice ALTER COLUMN memory_limit SET NOT NULL;

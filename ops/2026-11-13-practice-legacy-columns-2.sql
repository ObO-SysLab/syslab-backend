-- Legacy practice columns, step 2 of 2 (run AFTER the backend without these mappings is deployed;
-- production uses ddl-auto: validate, which accepts extra columns but not missing mapped ones).
-- os_image/cpu_limit/memory_limit held hidden form defaults never used by the VM (fixed 0.3 CPU,
-- 256 MB); allowed_commands held hidden defaults that never restricted anything (see command_allowlist).
-- Take a pg_dump first: the dropped values cannot be recovered otherwise.
BEGIN;
SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '30s';

ALTER TABLE problem_practice DROP COLUMN IF EXISTS os_image;
ALTER TABLE problem_practice DROP COLUMN IF EXISTS allowed_commands;
ALTER TABLE problem_practice DROP COLUMN IF EXISTS cpu_limit;
ALTER TABLE problem_practice DROP COLUMN IF EXISTS memory_limit;

COMMIT;

-- Verification (read-only): no legacy column remains.
-- SELECT column_name FROM information_schema.columns WHERE table_name = 'problem_practice'
--  AND column_name IN ('os_image', 'allowed_commands', 'cpu_limit', 'memory_limit');

-- Rollback: restore the columns (nullable) and their values from the pre-step dump if ever needed.

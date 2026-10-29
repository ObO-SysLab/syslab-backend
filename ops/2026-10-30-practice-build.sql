-- AUDIT-07: PREPARED ONLY. Approval, backup and a paused creation/consumer path required.
-- Run against the confirmed database/schema with ON_ERROR_STOP. Never rely on ddl-auto:update.
BEGIN;
SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '30s';

ALTER TABLE problem_practice ADD COLUMN IF NOT EXISTS build_request_id varchar(36);
ALTER TABLE problem_practice ADD COLUMN IF NOT EXISTS code_build_id varchar(255);
ALTER TABLE problem_practice ADD COLUMN IF NOT EXISTS build_requested_at timestamptz;
ALTER TABLE problem_practice ADD COLUMN IF NOT EXISTS build_deadline_at timestamptz;
ALTER TABLE problem_practice ADD COLUMN IF NOT EXISTS build_failure_reason varchar(40);

-- Old pending rows cannot be safely associated with a generation. Preserve READY/FAILED.
-- Approval includes this data change; keep an external backup of affected rows before executing.
UPDATE problem_practice SET image_status = 'FAILED', build_failure_reason = 'LEGACY_UNTRACKED'
WHERE image_status = 'PENDING' AND build_request_id IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_practice_build_request
ON problem_practice(build_request_id) WHERE build_request_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_practice_code_build
ON problem_practice(code_build_id) WHERE code_build_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS ix_practice_build_deadline
ON problem_practice(build_deadline_at) WHERE image_status = 'PENDING' AND build_request_id IS NOT NULL;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'problem_practice'::regclass AND conname = 'ck_practice_build_request') THEN
        ALTER TABLE problem_practice ADD CONSTRAINT ck_practice_build_request CHECK (
            (build_request_id IS NULL AND code_build_id IS NULL AND build_requested_at IS NULL AND build_deadline_at IS NULL)
            OR (build_request_id IS NOT NULL
                AND build_request_id ~ '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
                AND build_requested_at IS NOT NULL AND build_deadline_at IS NOT NULL
                AND build_deadline_at > build_requested_at
                AND image_status IS NOT NULL AND image_status IN ('PENDING', 'READY', 'FAILED')
                AND (image_status <> 'READY' OR (code_build_id IS NOT NULL AND ecr_image_uri IS NOT NULL
                    AND ecr_image_uri ~ '@sha256:[0-9a-f]{64}$')))
        );
    END IF;
END $$;
COMMIT;

-- Verification (read-only):
SELECT image_status, build_failure_reason, count(*) FROM problem_practice GROUP BY 1,2;
SELECT count(*) AS inconsistent_pending FROM problem_practice
WHERE image_status = 'PENDING' AND (build_request_id IS NULL OR build_deadline_at IS NULL);

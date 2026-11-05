-- Prepared only. Back up and inspect actual schema/data before approved execution.
-- Do not delete duplicate/unknown sessions automatically to make this migration pass.
BEGIN;
LOCK TABLE vm_sessions IN SHARE ROW EXCLUSIVE MODE;
DO $$ BEGIN
  IF EXISTS (SELECT 1 FROM vm_sessions WHERE status NOT IN ('RUNNING','STOPPED','CREATING','STOPPING')) THEN
    RAISE EXCEPTION 'Unknown VM states require review';
  END IF;
  IF EXISTS (SELECT user_id FROM vm_sessions WHERE status IN ('CREATING','RUNNING','STOPPING')
             GROUP BY user_id HAVING count(*) > 1) THEN
    RAISE EXCEPTION 'Duplicate active VM sessions require manual DB/Docker reconciliation';
  END IF;
END $$;
ALTER TABLE vm_sessions ADD COLUMN IF NOT EXISTS runtime_version integer NOT NULL DEFAULT 0;
CREATE UNIQUE INDEX IF NOT EXISTS ux_vm_sessions_active_user ON vm_sessions(user_id)
  WHERE status IN ('CREATING','RUNNING','STOPPING');
CREATE INDEX IF NOT EXISTS ix_vm_sessions_recovery ON vm_sessions(id)
  WHERE status IN ('CREATING','RUNNING','STOPPING');
COMMIT;

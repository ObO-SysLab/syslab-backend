-- Review and apply through the deployment migration process; never executed by the app.
BEGIN;
CREATE TABLE IF NOT EXISTS problem_revision (
    id varchar(96) PRIMARY KEY,
    problem_id bigint NOT NULL,
    snapshot jsonb NOT NULL,
    created_at timestamp NOT NULL
);
CREATE INDEX IF NOT EXISTS ix_problem_revision_problem ON problem_revision(problem_id);
ALTER TABLE solve_submission ADD COLUMN IF NOT EXISTS problem_revision_id varchar(96) REFERENCES problem_revision(id);
ALTER TABLE solve_submission ADD COLUMN IF NOT EXISTS obo_public_records jsonb;
ALTER TABLE contest_submission ADD COLUMN IF NOT EXISTS problem_revision_id varchar(96) REFERENCES problem_revision(id);
ALTER TABLE contest_submission ADD COLUMN IF NOT EXISTS objective_answer jsonb;
CREATE OR REPLACE FUNCTION preserve_obo_revision() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'Problem revisions are immutable';
END $$;
DROP TRIGGER IF EXISTS immutable_obo_revision ON problem_revision;
CREATE TRIGGER immutable_obo_revision BEFORE UPDATE OR DELETE ON problem_revision
    FOR EACH ROW EXECUTE FUNCTION preserve_obo_revision();
CREATE OR REPLACE FUNCTION preserve_submission_revision() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.problem_revision_id IS DISTINCT FROM NEW.problem_revision_id THEN
        RAISE EXCEPTION 'Submission revision binding is immutable';
    END IF;
    RETURN NEW;
END $$;
DROP TRIGGER IF EXISTS immutable_submission_revision ON solve_submission;
CREATE TRIGGER immutable_submission_revision BEFORE UPDATE ON solve_submission
    FOR EACH ROW EXECUTE FUNCTION preserve_submission_revision();
DROP TRIGGER IF EXISTS immutable_contest_submission_revision ON contest_submission;
CREATE TRIGGER immutable_contest_submission_revision BEFORE UPDATE ON contest_submission
    FOR EACH ROW EXECUTE FUNCTION preserve_submission_revision();
COMMIT;

-- ==========================================
-- Remove Spring Batch metadata (replaced by lightweight OpenCSV import tracking)
-- ==========================================

DROP TABLE IF EXISTS BATCH_JOB_EXECUTION_CONTEXT;
DROP TABLE IF EXISTS BATCH_STEP_EXECUTION_CONTEXT;
DROP TABLE IF EXISTS BATCH_STEP_EXECUTION;
DROP TABLE IF EXISTS BATCH_JOB_EXECUTION_PARAMS;
DROP TABLE IF EXISTS BATCH_JOB_EXECUTION;
DROP TABLE IF EXISTS BATCH_JOB_INSTANCE;

DROP SEQUENCE IF EXISTS BATCH_STEP_EXECUTION_SEQ;
DROP SEQUENCE IF EXISTS BATCH_JOB_EXECUTION_SEQ;
DROP SEQUENCE IF EXISTS BATCH_JOB_SEQ;

-- ==========================================
-- Import job tracking (replaces JobRepository/JobExplorer)
-- ==========================================

CREATE TABLE IF NOT EXISTS import_job (
    id UUID PRIMARY KEY,
    type VARCHAR(20) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    read_count BIGINT NOT NULL DEFAULT 0,
    write_count BIGINT NOT NULL DEFAULT 0,
    skip_count BIGINT NOT NULL DEFAULT 0,
    started_at TIMESTAMP,
    finished_at TIMESTAMP,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by UUID,
    last_modified_by UUID,
    version BIGINT,
    CONSTRAINT fk_import_job_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_import_job_last_modified_by FOREIGN KEY (last_modified_by) REFERENCES users(id)
);

CREATE INDEX idx_import_job_status ON import_job(status);

CREATE TABLE IF NOT EXISTS import_job_error (
    id UUID PRIMARY KEY,
    import_job_id UUID NOT NULL,
    row_number BIGINT NOT NULL,
    reason TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by UUID,
    last_modified_by UUID,
    version BIGINT,
    CONSTRAINT fk_import_job_error_job FOREIGN KEY (import_job_id) REFERENCES import_job(id),
    CONSTRAINT fk_import_job_error_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_import_job_error_last_modified_by FOREIGN KEY (last_modified_by) REFERENCES users(id)
);

CREATE INDEX idx_import_job_error_job ON import_job_error(import_job_id);

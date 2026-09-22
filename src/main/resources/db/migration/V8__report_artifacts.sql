-- ==========================================
-- Generated report artifacts (payroll bank files, attendance timesheets)
-- ==========================================

CREATE TABLE IF NOT EXISTS report (
    id UUID PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    format VARCHAR(20) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    payroll_run_id UUID,
    period_start DATE,
    period_end DATE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by UUID,
    last_modified_by UUID,
    version BIGINT,
    CONSTRAINT fk_report_payroll_run FOREIGN KEY (payroll_run_id) REFERENCES payroll_run(id),
    CONSTRAINT fk_report_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_report_last_modified_by FOREIGN KEY (last_modified_by) REFERENCES users(id)
);

CREATE INDEX idx_report_type ON report(type);
CREATE INDEX idx_report_payroll_run_id ON report(payroll_run_id);
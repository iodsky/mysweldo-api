CREATE TABLE IF NOT EXISTS overtime_request (
    id UUID PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    date Date NOT NULL,
    overtime_hours NUMERIC(19, 2),
    reason VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by UUID,
    last_modified_by UUID,
    version BIGINT,
    CONSTRAINT uk_employee_id_date UNIQUE(employee_id, date)
);
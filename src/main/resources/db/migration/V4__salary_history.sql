CREATE TABLE IF NOT EXISTS salary_history (
    id UUID PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    rate NUMERIC(19 ,2) NOT NULL,
    pay_type VARCHAR(50) NOT NULL,
    payroll_frequency VARCHAR(50) NOT NULL,
    effective_from DATE NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by UUID,
    last_modified_by UUID,
    version BIGINT,
    CONSTRAINT fk_salary_history_employee FOREIGN KEY (employee_id) REFERENCES employee(id),
    CONSTRAINT fk_salary_history_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_salary_history_last_modified_by FOREIGN KEY (last_modified_by) REFERENCES users(id)
);

CREATE INDEX idx_salary_history_employee_effective_from
    ON salary_history(employee_id, effective_from DESC);
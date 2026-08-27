ALTER TABLE leave_request DROP CONSTRAINT uk_leave_request_employee_dates;

CREATE UNIQUE INDEX uq_leave_request_employee_dates
    ON leave_request(employee_id, start_date, end_date)
    WHERE deleted_at IS NULL;

ALTER TABLE department DROP CONSTRAINT department_title_key;

CREATE UNIQUE INDEX uq_department_title
    ON department(title)
    WHERE deleted_at IS NULL;

ALTER TABLE position DROP CONSTRAINT position_title_key;

CREATE UNIQUE INDEX uq_position_title
    ON position(title)
    WHERE deleted_at IS NULL;


CREATE UNIQUE INDEX uq_payroll_run_regular_period
    ON payroll_run(period_start_date, period_end_date, type)
    WHERE type = 'REGULAR' AND deleted_at IS NULL;
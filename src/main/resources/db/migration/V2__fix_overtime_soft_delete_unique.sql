ALTER TABLE overtime_request DROP CONSTRAINT uk_employee_id_date;

CREATE UNIQUE INDEX uq_overtime_request_employee_date
    ON overtime_request(employee_id, date)
    WHERE deleted_at IS NULL;
CREATE TABLE IF NOT EXISTS department (
    id VARCHAR(20) PRIMARY KEY,
    title VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by UUID,
    last_modified_by UUID,
    version BIGINT
);

CREATE TABLE IF NOT EXISTS position (
    id VARCHAR(20) PRIMARY KEY,
    title VARCHAR(255) NOT NULL UNIQUE,
    department_id VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by UUID,
    last_modified_by UUID,
    version BIGINT,
    CONSTRAINT fk_position_department FOREIGN KEY (department_id) REFERENCES department(id)
);

CREATE TABLE IF NOT EXISTS deduction (
    code VARCHAR(255) PRIMARY KEY,
    description VARCHAR(255),
    statutory BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by UUID,
    last_modified_by UUID,
    version BIGINT
);

CREATE TABLE IF NOT EXISTS contribution (
    code VARCHAR(255) PRIMARY KEY,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by UUID,
    last_modified_by UUID,
    version BIGINT,
    CONSTRAINT uk_contribution_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS benefit (
    code VARCHAR(255) PRIMARY KEY,
    description VARCHAR(255),
    taxable BOOLEAN NOT NULL DEFAULT FALSE,
    non_taxable_limit NUMERIC(19, 2),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by UUID,
    last_modified_by UUID,
    version BIGINT
);

CREATE SEQUENCE roles_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS roles (
    id BIGINT PRIMARY KEY DEFAULT nextval('roles_id_seq'),
    name VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by UUID,
    last_modified_by UUID,
    version BIGINT,
    CONSTRAINT uk_roles_name UNIQUE (name)
);

CREATE SEQUENCE employee_id_seq START WITH 10001 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS employee (
    id BIGINT PRIMARY KEY DEFAULT nextval('employee_id_seq'),
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    birthday DATE,
    address VARCHAR(255) UNIQUE,
    phone_number VARCHAR(255) UNIQUE,
    supervisor_id BIGINT,
    position_id VARCHAR(20),
    department_id VARCHAR(20),
    status VARCHAR(50),
    type VARCHAR(50),
    start_shift TIME,
    end_shift TIME,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by UUID,
    last_modified_by UUID,
    version BIGINT
);

CREATE TABLE IF NOT EXISTS salary (
    id UUID PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    rate NUMERIC(19 ,2),
    pay_type VARCHAR(50),
    payroll_frequency VARCHAR(50),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by UUID,
    last_modified_by UUID,
    version BIGINT
);

CREATE TABLE IF NOT EXISTS government_id (
    id UUID PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    sss_no VARCHAR(255) UNIQUE,
    tin_no VARCHAR(255) UNIQUE,
    philhealth_no VARCHAR(255) UNIQUE,
    pagibig_no VARCHAR(255) UNIQUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by UUID,
    last_modified_by UUID,
    version BIGINT,
    CONSTRAINT fk_government_id_employee FOREIGN KEY (employee_id) REFERENCES employee(id)
);

CREATE TABLE IF NOT EXISTS employee_benefit (
    id UUID PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    benefit_code VARCHAR(255) NOT NULL,
    amount NUMERIC(19, 2),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by UUID,
    last_modified_by UUID,
    version BIGINT,
    CONSTRAINT fk_benefit_employee FOREIGN KEY (employee_id) REFERENCES employee(id),
    CONSTRAINT fk_benefit FOREIGN KEY (benefit_code) REFERENCES benefit(code)
);

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    email VARCHAR(255) UNIQUE,
    password VARCHAR(255),
    role_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by UUID,
    last_modified_by UUID,
    version BIGINT,
    CONSTRAINT fk_users_employee FOREIGN KEY (employee_id) REFERENCES employee(id),
    CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles(id)
);

-- Add foreign keys for created_by and last_modified_by in department
ALTER TABLE department
    ADD CONSTRAINT fk_department_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    ADD CONSTRAINT fk_department_last_modified_by FOREIGN KEY (last_modified_by) REFERENCES users(id);

-- Add foreign keys for created_by and last_modified_by in position
ALTER TABLE position
    ADD CONSTRAINT fk_position_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    ADD CONSTRAINT fk_position_last_modified_by FOREIGN KEY (last_modified_by) REFERENCES users(id);

-- Add foreign keys for created_by and last_modified_by in employee
ALTER TABLE employee
    ADD CONSTRAINT fk_employee_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    ADD CONSTRAINT fk_employee_last_modified_by FOREIGN KEY (last_modified_by) REFERENCES users(id),
    ADD CONSTRAINT fk_employee_supervisor FOREIGN KEY (supervisor_id) REFERENCES employee(id),
    ADD CONSTRAINT fk_employee_position FOREIGN KEY (position_id) REFERENCES position(id),
    ADD CONSTRAINT fk_employee_department FOREIGN KEY (department_id) REFERENCES department(id);

-- Add foreign keys for created_by and last_modified_by in government_id
ALTER TABLE government_id
    ADD CONSTRAINT fk_government_id_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    ADD CONSTRAINT fk_government_id_last_modified_by FOREIGN KEY (last_modified_by) REFERENCES users(id);

-- Add foreign keys for created_by and last_modified_by in benefit
ALTER TABLE employee_benefit
    ADD CONSTRAINT fk_employee_benefit_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    ADD CONSTRAINT fk_employee_benefit_last_modified_by FOREIGN KEY (last_modified_by) REFERENCES users(id);

-- Add foreign keys for created_by and last_modified_by in roles
ALTER TABLE roles
    ADD CONSTRAINT fk_roles_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    ADD CONSTRAINT fk_roles_last_modified_by FOREIGN KEY (last_modified_by) REFERENCES users(id);

-- Add foreign keys for created_by and last_modified_by in users
ALTER TABLE users
    ADD CONSTRAINT fk_users_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    ADD CONSTRAINT fk_users_last_modified_by FOREIGN KEY (last_modified_by) REFERENCES users(id);

-- Add foreign keys for created_by and last_modified_by in deduction_type
ALTER TABLE deduction
    ADD CONSTRAINT fk_deduction_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    ADD CONSTRAINT fk_deduction_last_modified_by FOREIGN KEY (last_modified_by) REFERENCES users(id);

-- Add foreign keys for created_by and last_modified_by in benefit
ALTER TABLE benefit
    ADD CONSTRAINT fk_benefit_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    ADD CONSTRAINT fk_benefit_last_modified_by FOREIGN KEY (last_modified_by) REFERENCES users(id);

ALTER TABLE contribution
    ADD CONSTRAINT fk_contribution_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    ADD CONSTRAINT fk_contribution_last_modified_by FOREIGN KEY (last_modified_by) REFERENCES users(id);

ALTER TABLE salary
    ADD CONSTRAINT fk_salary_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    ADD CONSTRAINT fk_salary_last_modified_by FOREIGN KEY (last_modified_by) REFERENCES users(id),
    ADD CONSTRAINT uk_salary_employee UNIQUE (employee_id);

CREATE TABLE IF NOT EXISTS attendance (
    id UUID PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    date DATE,
    time_in TIME,
    time_out TIME,
    total_hours NUMERIC(19, 2),
    overtime NUMERIC(19, 2),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by UUID,
    last_modified_by UUID,
    version BIGINT,
    CONSTRAINT fk_attendance_employee FOREIGN KEY (employee_id) REFERENCES employee(id),
    CONSTRAINT fk_attendance_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_attendance_last_modified_by FOREIGN KEY (last_modified_by) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS leave_credit (
    id UUID PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    type VARCHAR(50),
    credits DOUBLE PRECISION NOT NULL,
    effective_date DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by UUID,
    last_modified_by UUID,
    version BIGINT,
    CONSTRAINT fk_leave_credit_employee FOREIGN KEY (employee_id) REFERENCES employee(id),
    CONSTRAINT fk_leave_credit_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_leave_credit_last_modified_by FOREIGN KEY (last_modified_by) REFERENCES users(id),
    CONSTRAINT chk_leave_credit_positive CHECK (credits >= 0)
);

CREATE TABLE IF NOT EXISTS leave_request (
    id VARCHAR(255) PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    leave_type VARCHAR(50),
    start_date DATE,
    end_date DATE,
    note TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by UUID,
    last_modified_by UUID,
    version BIGINT,
    CONSTRAINT fk_leave_request_employee FOREIGN KEY (employee_id) REFERENCES employee(id),
    CONSTRAINT fk_leave_request_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_leave_request_last_modified_by FOREIGN KEY (last_modified_by) REFERENCES users(id),
    CONSTRAINT uk_leave_request_employee_dates UNIQUE (employee_id, start_date, end_date)
);

CREATE TABLE IF NOT EXISTS payroll_run (
    id UUID PRIMARY KEY,
    period_start_date DATE  NOT NULL,
    period_end_date DATE NOT NULL,
    payroll_frequency VARCHAR(50) NOT NULL DEFAULT 'SEMI_MONTHLY',
    type VARCHAR(50) NOT NULL DEFAULT 'REGULAR',
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    total_gross_pay NUMERIC(19, 2),
    total_net_pay NUMERIC(19, 2),
    total_deductions NUMERIC(19, 2),
    total_benefits NUMERIC (19, 2),
    total_employer_cost NUMERIC(19,2),
    notes VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by UUID,
    last_modified_by UUID,
    version BIGINT,
    CONSTRAINT fk_payroll_run_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_payroll_run_last_modified_by FOREIGN KEY(last_modified_by) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS payroll_item (
    id UUID PRIMARY KEY,
    payroll_run_id UUID,
    employee_id BIGINT NOT NULL,
    days_worked NUMERIC(5,2) NOT NULL,

    monthly_rate NUMERIC(19, 2),
    semi_monthly_rate NUMERIC(19, 2),
    daily_rate NUMERIC(19, 2),
    hourly_rate NUMERIC(19, 2),
    pay_type VARCHAR(50),

    absences NUMERIC(5,2),
    tardiness_minutes INTEGER,
    undertime_minutes INTEGER,
    overtime_minutes INTEGER,

    overtime_pay NUMERIC(19, 2),
    total_benefits NUMERIC(19, 2),
    gross_pay NUMERIC(19, 2),

    total_deductions NUMERIC(19, 2),
    net_pay NUMERIC(19, 2),

    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by UUID,
    last_modified_by UUID,
    version BIGINT,

    CONSTRAINT fk_payroll_run_payroll_item FOREIGN KEY (payroll_run_id) REFERENCES payroll_run(id),
    CONSTRAINT fk_payroll_item_employee FOREIGN KEY (employee_id) REFERENCES employee(id),
    CONSTRAINT fk_payroll_item_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_payroll_item_last_modified_by FOREIGN KEY (last_modified_by) REFERENCES users(id),
    CONSTRAINT uk_payroll_run_id_employee_id UNIQUE (payroll_run_id, employee_id)
);

CREATE TABLE IF NOT EXISTS employer_contribution (
    id UUID PRIMARY KEY,
    payroll_item_id UUID NOT NULL,
    contribution_code VARCHAR(255) NOT NULL,
    amount NUMERIC(19,2) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by UUID,
    last_modified_by UUID,
    version BIGINT,
    CONSTRAINT fk_contribution_payroll_item FOREIGN KEY (payroll_item_id) REFERENCES payroll_item(id),
    CONSTRAINT fk_contribution_code FOREIGN KEY (contribution_code) REFERENCES contribution(code),
    CONSTRAINT fk_contribution_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_contribution_last_modified_by FOREIGN KEY (last_modified_by) REFERENCES users(id),
    CONSTRAINT uk_employer_contribution UNIQUE (payroll_item_id, contribution_code)
);

CREATE TABLE IF NOT EXISTS payroll_deduction (
    id UUID PRIMARY KEY,
    payroll_item_id UUID NOT NULL,
    deduction_code VARCHAR(255) NOT NULL,
    amount NUMERIC(19, 2),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by UUID,
    last_modified_by UUID,
    version BIGINT,
    CONSTRAINT fk_payroll_deduction_payroll_item FOREIGN KEY (payroll_item_id) REFERENCES payroll_item(id),
    CONSTRAINT fk_payroll_deduction_deduction FOREIGN KEY (deduction_code) REFERENCES deduction (code),
    CONSTRAINT fk_payroll_deduction_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_payroll_deduction_last_modified_by FOREIGN KEY (last_modified_by) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS payroll_benefit (
    id UUID PRIMARY KEY,
    payroll_item_id UUID NOT NULL,
    benefit_code VARCHAR(255) NOT NULL,
    amount NUMERIC(19, 2),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by UUID,
    last_modified_by UUID,
    version BIGINT,
    CONSTRAINT fk_payroll_benefits_payroll_item FOREIGN KEY (payroll_item_id) REFERENCES payroll_item(id),
    CONSTRAINT fk_payroll_benefit FOREIGN KEY (benefit_code) REFERENCES benefit(code),
    CONSTRAINT fk_payroll_benefits_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_payroll_benefits_last_modified_by FOREIGN KEY (last_modified_by) REFERENCES users(id)
);

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

CREATE TABLE IF NOT EXISTS pagibig_rate (
    id UUID PRIMARY KEY,
    employee_rate DECIMAL(5,4) NOT NULL, -- 2%
    employer_rate DECIMAL(5,4) NOT NULL, -- 2%
    low_income_threshold NUMERIC DEFAULT 1500.00,
    low_income_employee_rate DECIMAL(5,4) DEFAULT 0.01,
    max_salary_cap NUMERIC NOT NULL DEFAULT 10000.00,
    effective_date DATE NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by UUID,
    last_modified_by UUID,
    version BIGINT
);

CREATE TABLE IF NOT EXISTS philhealth_rate (
    id UUID PRIMARY KEY,
    premium_rate DECIMAL(5, 4) NOT NULL, -- 0.05 (5%)
    max_salary_cap NUMERIC NOT NULL DEFAULT 100000.00,
    min_salary_floor NUMERIC NOT NULL DEFAULT 10000.00,
    fixed_contribution NUMERIC NOT NULL DEFAULT 500.00,
    effective_date DATE NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by UUID,
    last_modified_by UUID,
    version BIGINT
);

CREATE TABLE IF NOT EXISTS sss_rate (
    id UUID PRIMARY KEY,
    total_sss NUMERIC NOT NULL,
    employee_sss DECIMAL(5,4) NOT NULL,
    employer_sss DECIMAL(5,4) NOT NULL,
    salary_brackets JSONB NOT NULL,
    effective_date DATE NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by UUID,
    last_modified_by UUID,
    version BIGINT
);

CREATE TABLE IF NOT EXISTS tax_bracket (
    id UUID PRIMARY KEY,
    min_income NUMERIC NOT NULL,
    max_income NUMERIC,
    base_tax NUMERIC NOT NULL DEFAULT 0.00,
    marginal_rate DECIMAL(5,4) NOT NULL, -- e.g., 0.15
    threshold NUMERIC NOT NULL,
    effective_date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by UUID,
    last_modified_by UUID,
    version BIGINT,
    UNIQUE (effective_date, min_income)
);

-- ==========================================
-- SPRING BATCH SCHEMA
-- ==========================================
CREATE TABLE BATCH_JOB_INSTANCE (
    JOB_INSTANCE_ID BIGINT  NOT NULL PRIMARY KEY,
    VERSION BIGINT,
    JOB_NAME VARCHAR(100) NOT NULL,
    JOB_KEY VARCHAR(32) NOT NULL,
    constraint JOB_INST_UN unique (JOB_NAME, JOB_KEY)
);

CREATE TABLE BATCH_JOB_EXECUTION (
    JOB_EXECUTION_ID BIGINT  NOT NULL PRIMARY KEY,
    VERSION BIGINT,
    JOB_INSTANCE_ID BIGINT NOT NULL,
    CREATE_TIME TIMESTAMP NOT NULL,
    START_TIME TIMESTAMP DEFAULT NULL,
    END_TIME TIMESTAMP DEFAULT NULL,
    STATUS VARCHAR(10),
    EXIT_CODE VARCHAR(2500),
    EXIT_MESSAGE VARCHAR(2500),
    LAST_UPDATED TIMESTAMP,
    constraint JOB_INST_EXEC_FK foreign key (JOB_INSTANCE_ID)
        references BATCH_JOB_INSTANCE(JOB_INSTANCE_ID)
);

CREATE TABLE BATCH_JOB_EXECUTION_PARAMS (
    JOB_EXECUTION_ID BIGINT NOT NULL,
    PARAMETER_NAME VARCHAR(100) NOT NULL,
    PARAMETER_TYPE VARCHAR(100) NOT NULL,
    PARAMETER_VALUE VARCHAR(2500),
    IDENTIFYING CHAR(1) NOT NULL,
    constraint JOB_EXEC_PARAMS_FK foreign key (JOB_EXECUTION_ID)
        references BATCH_JOB_EXECUTION(JOB_EXECUTION_ID)
);

CREATE TABLE BATCH_STEP_EXECUTION (
    STEP_EXECUTION_ID BIGINT  NOT NULL PRIMARY KEY,
    VERSION BIGINT NOT NULL,
    STEP_NAME VARCHAR(100) NOT NULL,
    JOB_EXECUTION_ID BIGINT NOT NULL,
    CREATE_TIME TIMESTAMP NOT NULL,
    START_TIME TIMESTAMP DEFAULT NULL,
    END_TIME TIMESTAMP DEFAULT NULL,
    STATUS VARCHAR(10),
    COMMIT_COUNT BIGINT,
    READ_COUNT BIGINT,
    FILTER_COUNT BIGINT,
    WRITE_COUNT BIGINT,
    READ_SKIP_COUNT BIGINT,
    WRITE_SKIP_COUNT BIGINT,
    PROCESS_SKIP_COUNT BIGINT,
    ROLLBACK_COUNT BIGINT,
    EXIT_CODE VARCHAR(2500),
    EXIT_MESSAGE VARCHAR(2500),
    LAST_UPDATED TIMESTAMP,
    constraint JOB_EXEC_STEP_FK foreign key (JOB_EXECUTION_ID)
        references BATCH_JOB_EXECUTION(JOB_EXECUTION_ID)
);

CREATE TABLE BATCH_STEP_EXECUTION_CONTEXT (
    STEP_EXECUTION_ID BIGINT NOT NULL PRIMARY KEY,
    SHORT_CONTEXT VARCHAR(2500) NOT NULL,
    SERIALIZED_CONTEXT TEXT,
    constraint STEP_EXEC_CTX_FK foreign key (STEP_EXECUTION_ID)
        references BATCH_STEP_EXECUTION(STEP_EXECUTION_ID)
);

CREATE TABLE BATCH_JOB_EXECUTION_CONTEXT (
    JOB_EXECUTION_ID BIGINT NOT NULL PRIMARY KEY,
    SHORT_CONTEXT VARCHAR(2500) NOT NULL,
    SERIALIZED_CONTEXT TEXT,
    constraint JOB_EXEC_CTX_FK foreign key (JOB_EXECUTION_ID)
        references BATCH_JOB_EXECUTION(JOB_EXECUTION_ID)
);

-- Spring Batch 5.x uses simplified sequence naming
CREATE SEQUENCE BATCH_STEP_EXECUTION_SEQ MAXVALUE 9223372036854775807 NO CYCLE;
CREATE SEQUENCE BATCH_JOB_EXECUTION_SEQ MAXVALUE 9223372036854775807 NO CYCLE;
CREATE SEQUENCE BATCH_JOB_SEQ MAXVALUE 9223372036854775807 NO CYCLE;

CREATE INDEX idx_employee_first_name ON employee(first_name);
CREATE INDEX idx_employee_last_name ON employee(last_name);
CREATE INDEX idx_employee_supervisor ON employee(supervisor_id);
CREATE INDEX idx_employee_position ON employee(position_id);
CREATE INDEX idx_employee_department ON employee(department_id);
CREATE INDEX idx_benefit_employee ON employee_benefit (employee_id);
CREATE INDEX idx_attendance_date ON attendance(date);
CREATE INDEX idx_attendance_employee_date ON attendance(employee_id, date);
CREATE INDEX idx_leave_request_employee ON leave_request(employee_id);
CREATE INDEX idx_leave_request_dates ON leave_request(start_date, end_date);
CREATE INDEX idx_payroll_item_employee ON payroll_item(employee_id);
CREATE INDEX idx_payroll_run_period_status ON payroll_run(period_start_date, period_end_date, status);
-- ==========================================
-- REFERENCE DATA
-- ==========================================

INSERT INTO department (id, title, created_at, updated_at, version) VALUES
    ('IT', 'INFORMATION TECHNOLOGY', NOW(), NOW(), 0),
    ('HR', 'HUMAN RESOURCES', NOW(), NOW(), 0),
    ('CORP', 'CORPORATE', NOW(), NOW(), 0),
    ('ACC', 'ACCOUNTING', NOW(), NOW(), 0),
    ('PAY', 'PAYROLL', NOW(), NOW(), 0),
    ('SAL', 'SALES', NOW(), NOW(), 0),
    ('LOG', 'LOGISTICS', NOW(), NOW(), 0),
    ('CS', 'CUSTOMER SERVICE', NOW(), NOW(), 0);

INSERT INTO position (id, title, department_id, created_at, updated_at, version) VALUES
    ('CEO', 'Chief Executive Officer', 'CORP', NOW(), NOW(), 0),
    ('COO', 'Chief Operating Officer', 'CORP', NOW(), NOW(), 0),
    ('CFO', 'Chief Finance Officer', 'CORP', NOW(), NOW(), 0),
    ('CMO', 'Chief Marketing Officer', 'CORP', NOW(), NOW(), 0),
    ('CSR', 'Customer Service and Relations', 'CS', NOW(), NOW(), 0),
    ('ITOPSYS', 'IT Operations and Systems', 'IT', NOW(), NOW(), 0),
    ('HRM', 'HR Manager', 'HR', NOW(), NOW(), 0),
    ('HRTL', 'HR Team Leader', 'HR', NOW(), NOW(), 0),
    ('HRRL', 'HR Rank and File', 'HR', NOW(), NOW(), 0),
    ('ACCHEAD', 'Accounting Head', 'ACC', NOW(), NOW(), 0),
    ('ACCMNGR', 'Account Manager', 'ACC', NOW(), NOW(), 0),
    ('ACCTL', 'Account Team Leader', 'ACC', NOW(), NOW(), 0),
    ('ACCRL', 'Account Rank and File', 'ACC', NOW(), NOW(), 0),
    ('PAYRMNGR', 'Payroll Manager', 'PAY', NOW(), NOW(), 0),
    ('PAYRL', 'Payroll Rank and File', 'PAY', NOW(), NOW(), 0),
    ('PAYTL', 'Payroll Team Leader', 'PAY', NOW(), NOW(), 0),
    ('SCL', 'Supply Chain and Logistics', 'LOG', NOW(), NOW(), 0),
    ('SLMKT', 'Sales and Marketing', 'SAL', NOW(), NOW(), 0);

INSERT INTO deduction (code, description, created_at, updated_at, version) VALUES
    ('SSS', 'Social Security System', NOW(), NOW(), 0),
    ('PHIC', 'PhilHealth', NOW(), NOW(), 0),
    ('HDMF', 'Pag-IBIG', NOW(), NOW(), 0),
    ('TAX', 'Withholding Tax', NOW(), NOW(), 0);

INSERT INTO benefit (code, description, taxable, non_taxable_limit, created_at, updated_at, version) VALUES
    ('MEAL', 'MEAL ALLOWANCE', FALSE, 2500, NOW(), NOW(), 0),
    ('PHONE', 'PHONE ALLOWANCE', TRUE, NULL, NOW(), NOW(), 0),
    ('CLOTHING', 'CLOTHING ALLOWANCE', FALSE, 5000, NOW(), NOW(), 0);

INSERT INTO tax_bracket (id, min_income, max_income, base_tax, marginal_rate, threshold, effective_date, created_at, version) VALUES
-- 0% Tax
(GEN_RANDOM_UUID(), 0.00, 20833.00, 0.00, 0.0000, 0.00, '2026-01-01', CURRENT_TIMESTAMP, 0),
-- 15% of excess over 20,833
(GEN_RANDOM_UUID(), 20833.01, 33333.00, 0.00, 0.1500, 20833.00, '2026-01-01', CURRENT_TIMESTAMP, 0),
-- 1,875 + 20% of excess over 33,333
(GEN_RANDOM_UUID(), 33333.01, 66667.00, 1875.00, 0.2000, 33333.00, '2026-01-01', CURRENT_TIMESTAMP, 0),
-- 8,541.80 + 25% of excess over 66,667
(GEN_RANDOM_UUID(), 66667.01, 166667.00, 8541.80, 0.2500, 66667.00, '2026-01-01', CURRENT_TIMESTAMP, 0),
-- 33,541.80 + 30% of excess over 166,667
(GEN_RANDOM_UUID(), 166667.01, 666667.00, 33541.80, 0.3000, 166667.00, '2026-01-01', CURRENT_TIMESTAMP, 0),
-- 183,541.80 + 35% of excess over 666,667
(GEN_RANDOM_UUID(), 666667.01, NULL, 183541.80, 0.3500, 666667.00, '2026-01-01', CURRENT_TIMESTAMP, 0);

INSERT INTO philhealth_rate (id, premium_rate, max_salary_cap, min_salary_floor, fixed_contribution, effective_date, created_at, version) VALUES
    (GEN_RANDOM_UUID(), 0.0500,100000.00, 10000.00, 500.00, '2026-01-01',  CURRENT_TIMESTAMP, 0);

INSERT INTO pagibig_rate (id, employee_rate, employer_rate, low_income_threshold, low_income_employee_rate, max_salary_cap, effective_date, created_at, version) VALUES
    (GEN_RANDOM_UUID(), 0.0200, 0.0200, 1500.00, 0.0100, 10000.00, '2026-01-01', CURRENT_TIMESTAMP, 0);

INSERT INTO sss_rate (id, total_sss, employee_sss, employer_sss, salary_brackets, effective_date, created_at, version)
VALUES (GEN_RANDOM_UUID(), 0.15, 0.0500, 0.1000,
           '[
             {"msc": 5000, "maxSalary": 5249.99, "minSalary": 0.00},
             {"msc": 5500, "maxSalary": 5749.99, "minSalary": 5250.00},
             {"msc": 6000, "maxSalary": 6249.99, "minSalary": 5750.00},
             {"msc": 6500, "maxSalary": 6749.99, "minSalary": 6250.00},
             {"msc": 7000, "maxSalary": 7249.99, "minSalary": 6750.00},
             {"msc": 7500, "maxSalary": 7749.99, "minSalary": 7250.00},
             {"msc": 8000, "maxSalary": 8249.99, "minSalary": 7750.00},
             {"msc": 8500, "maxSalary": 8749.99, "minSalary": 8250.00},
             {"msc": 9000, "maxSalary": 9249.99, "minSalary": 8750.00},
             {"msc": 9500, "maxSalary": 9749.99, "minSalary": 9250.00},
             {"msc": 10000, "maxSalary": 10249.99, "minSalary": 9750.00},
             {"msc": 10500, "maxSalary": 10749.99, "minSalary": 10250.00},
             {"msc": 11000, "maxSalary": 11249.99, "minSalary": 10750.00},
             {"msc": 11500, "maxSalary": 11749.99, "minSalary": 11250.00},
             {"msc": 12000, "maxSalary": 12249.99, "minSalary": 11750.00},
             {"msc": 12500, "maxSalary": 12749.99, "minSalary": 12250.00},
             {"msc": 13000, "maxSalary": 13249.99, "minSalary": 12750.00},
             {"msc": 13500, "maxSalary": 13749.99, "minSalary": 13250.00},
             {"msc": 14000, "maxSalary": 14249.99, "minSalary": 13750.00},
             {"msc": 14500, "maxSalary": 14749.99, "minSalary": 14250.00},
             {"msc": 15000, "maxSalary": 15249.99, "minSalary": 14750.00},
             {"msc": 15500, "maxSalary": 15749.99, "minSalary": 15250.00},
             {"msc": 16000, "maxSalary": 16249.99, "minSalary": 15750.00},
             {"msc": 16500, "maxSalary": 16749.99, "minSalary": 16250.00},
             {"msc": 17000, "maxSalary": 17249.99, "minSalary": 16750.00},
             {"msc": 17500, "maxSalary": 17749.99, "minSalary": 17250.00},
             {"msc": 18000, "maxSalary": 18249.99, "minSalary": 17750.00},
             {"msc": 18500, "maxSalary": 18749.99, "minSalary": 18250.00},
             {"msc": 19000, "maxSalary": 19249.99, "minSalary": 18750.00},
             {"msc": 19500, "maxSalary": 19749.99, "minSalary": 19250.00},
             {"msc": 20000, "maxSalary": 20249.99, "minSalary": 19750.00},
             {"msc": 20500, "maxSalary": 20749.99, "minSalary": 20250.00},
             {"msc": 21000, "maxSalary": 21249.99, "minSalary": 20750.00},
             {"msc": 21500, "maxSalary": 21749.99, "minSalary": 21250.00},
             {"msc": 22000, "maxSalary": 22249.99, "minSalary": 21750.00},
             {"msc": 22500, "maxSalary": 22749.99, "minSalary": 22250.00},
             {"msc": 23000, "maxSalary": 23249.99, "minSalary": 22750.00},
             {"msc": 23500, "maxSalary": 23749.99, "minSalary": 23250.00},
             {"msc": 24000, "maxSalary": 24249.99, "minSalary": 23750.00},
             {"msc": 24500, "maxSalary": 24749.99, "minSalary": 24250.00},
             {"msc": 25000, "maxSalary": 25249.99, "minSalary": 24750.00},
             {"msc": 25500, "maxSalary": 25749.99, "minSalary": 25250.00},
             {"msc": 26000, "maxSalary": 26249.99, "minSalary": 25750.00},
             {"msc": 26500, "maxSalary": 26749.99, "minSalary": 26250.00},
             {"msc": 27000, "maxSalary": 27249.99, "minSalary": 26750.00},
             {"msc": 27500, "maxSalary": 27749.99, "minSalary": 27250.00},
             {"msc": 28000, "maxSalary": 28249.99, "minSalary": 27750.00},
             {"msc": 28500, "maxSalary": 28749.99, "minSalary": 28250.00},
             {"msc": 29000, "maxSalary": 29249.99, "minSalary": 28750.00},
             {"msc": 29500, "maxSalary": 29749.99, "minSalary": 29250.00},
             {"msc": 30000, "maxSalary": 30249.99, "minSalary": 29750.00},
             {"msc": 30500, "maxSalary": 30749.99, "minSalary": 30250.00},
             {"msc": 31000, "maxSalary": 31249.99, "minSalary": 30750.00},
             {"msc": 31500, "maxSalary": 31749.99, "minSalary": 31250.00},
             {"msc": 32000, "maxSalary": 32249.99, "minSalary": 31750.00},
             {"msc": 32500, "maxSalary": 32749.99, "minSalary": 32250.00},
             {"msc": 33000, "maxSalary": 33249.99, "minSalary": 32750.00},
             {"msc": 33500, "maxSalary": 33749.99, "minSalary": 33250.00},
             {"msc": 34000, "maxSalary": 34249.99, "minSalary": 33750.00},
             {"msc": 34500, "maxSalary": 34749.99, "minSalary": 34250.00},
             {"msc": 35000, "maxSalary": null, "minSalary": 34750.00}
           ]',
            '2026-01-01',
            '2026-03-14 08:51:05.965609',
            0
       );

INSERT INTO roles (name, created_at, updated_at, version) VALUES
    ('SUPERUSER', NOW(), NOW(), 0),
    ('IT', NOW(), NOW(), 0),
    ('HR', NOW(), NOW(), 0),
    ('PAYROLL', NOW(), NOW(), 0),
    ('EMPLOYEE', NOW(), NOW(), 0),
    ('SUPERVISOR', NOW(), NOW(), 0);

INSERT INTO contribution (code, description, created_at, updated_at, version) VALUES
    ('SSS_ER',  'SSS Employer Share',        NOW(), NOW(), 0),
    ('PHIC_ER', 'PhilHealth Employer Share',  NOW(), NOW(), 0),
    ('HDMF_ER', 'Pag-IBIG Employer Share',    NOW(), NOW(), 0);

INSERT INTO employee (id, first_name, last_name, birthday, address, phone_number, supervisor_id, position_id, department_id, status, created_at, updated_at, version) VALUES
    (10000, 'Super', 'User', '1990-01-15', '123 Test Street, Manila, Philippines', '+639171234567', NULL, 'ITOPSYS', 'IT', 'REGULAR', NOW(), NOW(), 0);

INSERT INTO government_id (id, employee_id, sss_no, tin_no, philhealth_no, pagibig_no, created_at, updated_at, version) VALUES
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 10000, '34-1234567-8', '123-456-789-000', '12-345678901-2', '1234-5678-9012', NOW(), NOW(), 0);

INSERT INTO users (id, employee_id, email, password, role_id, created_at, updated_at, version) VALUES
    ('d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 10000, 'superuser@sweldox.com', '$2a$12$lMIUx49rQdGhsrfLbQB3Hetueio4UgmdWV/Vcw3KweucDgZ6fDs/a', (SELECT id FROM roles WHERE name = 'SUPERUSER'), NOW(), NOW(), 0)
    ON CONFLICT (id) DO NOTHING;


-- Drop the fiscal_year column
ALTER TABLE leave_credit DROP COLUMN fiscal_year;

-- Add effective_date column as NOT NULL with default value
ALTER TABLE leave_credit ADD COLUMN effective_date DATE NOT NULL DEFAULT CURRENT_DATE;


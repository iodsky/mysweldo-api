ALTER TABLE attendance
    ALTER COLUMN time_in TYPE TIMESTAMP USING date + time_in,
    ALTER COLUMN time_out TYPE TIMESTAMP USING date + time_out,
    DROP COLUMN date,
    DROP COLUMN overtime;
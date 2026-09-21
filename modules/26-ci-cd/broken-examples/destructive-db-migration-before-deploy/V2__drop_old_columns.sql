-- V2: Refactor customer full_name into first_name and last_name
ALTER TABLE customers ADD COLUMN first_name VARCHAR(100) NOT NULL DEFAULT '';
ALTER TABLE customers ADD COLUMN last_name VARCHAR(100) NOT NULL DEFAULT '';

-- Immediately drop legacy column full_name
ALTER TABLE customers DROP COLUMN full_name;

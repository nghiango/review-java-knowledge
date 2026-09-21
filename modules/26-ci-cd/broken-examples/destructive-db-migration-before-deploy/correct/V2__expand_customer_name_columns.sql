-- Step 1: EXPAND - Add new columns as nullable so existing v1 inserts succeed without failure
ALTER TABLE customers ADD COLUMN IF NOT EXISTS first_name VARCHAR(100);
ALTER TABLE customers ADD COLUMN IF NOT EXISTS last_name VARCHAR(100);

-- Trigger to synchronize writes from v1 (which writes full_name) to new columns
CREATE OR REPLACE FUNCTION sync_customer_name_expand()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.first_name IS NULL AND NEW.full_name IS NOT NULL THEN
        NEW.first_name := split_part(NEW.full_name, ' ', 1);
        NEW.last_name  := substr(NEW.full_name, length(split_part(NEW.full_name, ' ', 1)) + 2);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_sync_customer_name_expand ON customers;
CREATE TRIGGER trg_sync_customer_name_expand
BEFORE INSERT OR UPDATE ON customers
FOR EACH ROW EXECUTE FUNCTION sync_customer_name_expand();

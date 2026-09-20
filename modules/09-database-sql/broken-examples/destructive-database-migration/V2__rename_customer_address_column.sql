-- Flyway Migration V2 (Destructive Breaking Change)
-- Immediately renaming a column causes all active/in-flight V1 application instances to fail immediately upon migration execution during rolling deployment.
ALTER TABLE customers RENAME COLUMN full_address TO delivery_address;

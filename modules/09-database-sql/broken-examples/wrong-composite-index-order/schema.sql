-- Multi-tenant Audit Log Schema
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    action VARCHAR(128) NOT NULL,
    details TEXT
);

-- Suboptimal composite index: leading column is low-cardinality status, which prevents efficient index lookups when status is omitted in WHERE clause
CREATE INDEX idx_audit_status_created_tenant ON audit_logs(status, created_at, tenant_id);

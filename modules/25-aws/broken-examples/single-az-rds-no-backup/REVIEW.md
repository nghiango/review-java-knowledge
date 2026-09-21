# Code Review: Production RDS PostgreSQL Terraform Configuration

## Pull Request Description
This PR provisions the primary Amazon RDS PostgreSQL database for our production Order Management System using Terraform.
The author selected a `db.r6g.xlarge` instance class with 100 GB allocated storage and mentions:
- `multi_az` was kept `false` to reduce infrastructure costs by 50% for initial launch.
- `publicly_accessible` was set to `true` to allow our distributed developers and BI team to connect directly from their laptops using DBeaver without having to set up a VPN or SSH bastion tunnel.
- `backup_retention_period` was set to `0` to prevent Amazon S3 backup storage fees during early deployment.
- `skip_final_snapshot` was set to `true` to prevent Terraform teardown hangs during pipeline testing.

## Files Under Review
- `rds.tf` — Terraform configuration for production PostgreSQL RDS instance.

## Review Questions
1. What is the SLA, MTTR, and operational impact of running a single-AZ instance in production?
2. What are the disaster recovery implications of `backup_retention_period = 0` and `skip_final_snapshot = true`?
3. What security risks arise from `publicly_accessible = true` and `storage_encrypted = false`?
4. How would you refactor this configuration to satisfy production enterprise reliability, disaster recovery, and compliance standards?

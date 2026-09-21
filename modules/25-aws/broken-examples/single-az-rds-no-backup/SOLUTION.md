# Solution: Production RDS PostgreSQL Terraform Configuration

## Annotated Target

```hcl
resource "aws_db_instance" "production_postgres" {
  identifier        = "orders-db-production"
  engine            = "postgres"
  engine_version    = "16.3"
  instance_class    = "db.r6g.xlarge"
  allocated_storage = 100

  db_name  = "orders"
  username = "orders_admin"
  password = var.db_password
  port     = 5432

  # Reliability issue: Single-AZ deployment (multi_az = false) offers no automatic failover during host or AZ outages.
  multi_az                = false
  # Security issue: Publicly accessible database allows direct internet traffic, exposing PostgreSQL port to brute-force and CVE attacks.
  publicly_accessible     = true
  # Security issue: Unencrypted storage (storage_encrypted = false) violates compliance mandates (SOC2, HIPAA, PCI-DSS).
  storage_encrypted       = false
  # Reliability issue: Disabling automated backups (backup_retention_period = 0) eliminates Point-In-Time-Recovery (PITR).
  backup_retention_period = 0
  # Reliability issue: Skipping final snapshot on deletion risks permanent data loss on accidental Terraform destroy.
  skip_final_snapshot     = true
  # Reliability issue: Disabled deletion protection allows accidental destruction via CLI or Terraform.
  deletion_protection     = false

  # Maintenance issue: Disabling auto minor version upgrades prevents automated security patches.
  auto_minor_version_upgrade = false

  vpc_security_group_ids = [aws_security_group.db_sg.id]
  db_subnet_group_name   = aws_db_subnet_group.db_subnets.name

  tags = {
    Environment = "production"
    Service     = "order-service"
  }
}
```

## Discovered Issues

### Reliability issue: Single-AZ deployment lacks automated failover
In single-AZ RDS deployments, the database instance operates on a single physical host within one Availability Zone. If the underlying EC2 hypervisor fails, the EBS volume degrades, or the AZ experiences power/networking degradation:
- AWS must spin up a replacement host, reattach storage, and perform crash recovery. Recovery takes between 15 and 35+ minutes, during which the application experiences complete outage.
- In contrast, a Multi-AZ deployment maintains a synchronous standby replica in a secondary AZ with automated DNS failover completed in 60–120 seconds with zero data loss ($RPO = 0$).

### Reliability issue: Disabling automated backups eliminates Point-In-Time-Recovery (PITR)
Setting `backup_retention_period = 0` disables continuous WAL archiving to S3. In the event of:
- Malicious database tampering or SQL injection,
- Erroneous database migration script (`DROP TABLE` or unintended `DELETE`),
- Physical data block corruption,
The organization has zero recovery points. Production databases must maintain at least 7–35 days of retention for continuous PITR to any second within the retention window.

### Security issue: Publicly accessible database exposes PostgreSQL to the Internet
Setting `publicly_accessible = true` assigns a public IPv4 address to the database instance. Even if security group rules restrict source IPs, internet-facing databases are vulnerable to misconfigured security groups, public IP address spoofing, and zero-day vulnerabilities in the PostgreSQL networking stack. Databases must strictly reside in private isolated subnets accessible solely via VPC peering, AWS Transit Gateway, or an AWS Systems Manager (SSM) Session Manager bastion tunnel.

### Security issue: Disabled storage encryption violates data protection regulations
Setting `storage_encrypted = false` stores database blocks and transaction logs in plaintext on underlying AWS EBS storage volumes. Encryption at rest must be enforced using AWS KMS Customer Managed Keys (CMK) with automatic annual key rotation.

---

## Correct Implementation

See [`correct/rds.tf`](correct/rds.tf) for the production-grade Terraform configuration.

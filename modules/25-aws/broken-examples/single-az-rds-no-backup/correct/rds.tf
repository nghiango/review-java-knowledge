resource "aws_db_instance" "production_postgres" {
  identifier        = "orders-db-production"
  engine            = "postgres"
  engine_version    = "16.3"
  instance_class    = "db.r6g.xlarge"
  allocated_storage = 100
  max_allocated_storage = 500
  storage_type      = "gp3"

  db_name  = "orders"
  username = "orders_admin"
  password = var.db_password
  port     = 5432

  multi_az                = true
  publicly_accessible     = false
  storage_encrypted       = true
  kms_key_id              = aws_kms_key.rds_cmk.arn
  backup_retention_period = 30
  backup_window           = "03:00-04:00"
  maintenance_window      = "Sun:04:30-Sun:05:30"
  skip_final_snapshot     = false
  final_snapshot_identifier = "orders-db-production-final-snapshot"
  deletion_protection     = true

  auto_minor_version_upgrade = true
  copy_tags_to_snapshot      = true

  enabled_cloudwatch_logs_exports = ["postgresql", "upgrade"]

  vpc_security_group_ids = [aws_security_group.db_sg.id]
  db_subnet_group_name   = aws_db_subnet_group.db_subnets.name

  tags = {
    Environment = "production"
    Service     = "order-service"
  }
}

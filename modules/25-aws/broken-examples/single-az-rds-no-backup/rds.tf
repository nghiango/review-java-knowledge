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

  multi_az                = false
  publicly_accessible     = true
  storage_encrypted       = false
  backup_retention_period = 0
  skip_final_snapshot     = true
  deletion_protection     = false

  auto_minor_version_upgrade = false

  vpc_security_group_ids = [aws_security_group.db_sg.id]
  db_subnet_group_name   = aws_db_subnet_group.db_subnets.name

  tags = {
    Environment = "production"
    Service     = "order-service"
  }
}

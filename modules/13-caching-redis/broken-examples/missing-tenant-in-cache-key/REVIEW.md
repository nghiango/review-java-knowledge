# Code Review — Cross-Tenant Cache Leakage

## Context

A multi-tenant SaaS accounting application provides tenant financial reporting endpoints. The service uses Spring Cache to cache generated report objects in Redis.

Review `TenantReportService.java` for multi-tenant isolation breaches, data leakage, and cache key collision vulnerabilities.

## What to look for

- Cache key definition and SpEL parameter resolution
- Multi-tenant data segregation in shared cache stores
- Privacy and confidentiality implications of cross-tenant key collisions

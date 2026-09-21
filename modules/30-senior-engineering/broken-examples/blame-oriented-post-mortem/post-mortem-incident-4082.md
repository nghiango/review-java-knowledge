# Incident 4082 Post-Mortem: Production Database Outage

**Date:** 2026-02-14
**Author:** QA & Release Manager

## Executive Summary
On Friday afternoon, our production PostgreSQL database crashed, resulting in 42 minutes of downtime for our web and mobile applications. The root cause was developer negligence: Junior Engineer Alex forgot to add a database index when running a manual SQL migration script in production, causing full table scans that locked up the database.

## Incident Timeline
- 16:15 — Alex ran a manual SQL migration script directly against the production database.
- 16:20 — Customers started complaining on Twitter that checkout was down.
- 16:25 — Lead Architect Bob noticed the database CPU was at 100%.
- 16:35 — Bob identified the slow query and killed Alex's connection.
- 16:57 — Database rebooted and services recovered.

## Root Cause
Alex failed to test the migration script on staging before applying it to production. Furthermore, Alex did not ask a senior developer for approval before executing the script.

## Action Items
1. Alex must be retrained on database fundamentals and attend SQL query optimization training.
2. All engineers are reminded to be more careful when executing production commands.
3. Developers must write down in Slack before running any database script.

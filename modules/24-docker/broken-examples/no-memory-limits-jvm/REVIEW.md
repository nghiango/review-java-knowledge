# Code Review — JVM Running in Container Without Memory Limits

## Context

A high-throughput order service is deployed to production via Docker Compose on a host node with 64 GB of physical RAM. The container configuration and startup script were authored by an infrastructure engineer.

Review `docker-compose.yml` and `entrypoint.sh` for cgroup memory limits, JVM ergonomics, heap sizing, and Linux Out-Of-Memory (OOM) killer risks.

## What to look for

- Absence of Docker container memory limits (`mem_limit` / `deploy.resources.limits.memory`)
- JVM heap ergonomics default calculations (`-XX:MaxRAMPercentage` vs default 25% of host RAM)
- Native memory footprint (Metaspace, thread stacks, direct buffers, Netty off-heap) vs container cgroup ceiling
- Kernel OOM Killer termination behavior (`Exit Code 137`)

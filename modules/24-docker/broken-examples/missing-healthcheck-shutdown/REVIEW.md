# Code Review — Shell Form Entrypoint Blocking SIGTERM and Missing Healthcheck

## Context

A production checkout service running on Docker frequently drops in-flight customer transactions during rolling deployments and container redeployments. Orchestration tools (Docker swarm, Kubernetes, ECS) report that containers take 10 seconds to terminate before being killed forcefully.

Review `Dockerfile` for Unix signal propagation, PID 1 execution model, Spring graceful shutdown, and container health verification.

## What to look for

- Shell form (`ENTRYPOINT command param`) vs Exec form (`ENTRYPOINT ["cmd", "param"]`)
- PID 1 assignment and `SIGTERM` forwarding to the child Java process
- Graceful shutdown timeout windows (`server.shutdown=graceful`)
- Container health monitoring (`HEALTHCHECK` directive) vs Actuator probes

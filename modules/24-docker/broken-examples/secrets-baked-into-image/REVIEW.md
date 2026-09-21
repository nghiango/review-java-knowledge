# Code Review — Secrets Baked into Docker Image

## Context

A microservice Dockerfile configures database credentials and a private GitHub token to clone private repositories during the Docker build process.

Review `Dockerfile` for build argument leakage, layer history persistence, credential exposure, and secret hygiene.

## What to look for

- Usage of `ARG` and `ENV` for sensitive API tokens and passwords
- Persistence of build-time secrets in intermediate image layers and manifest metadata
- Exposure via `docker history <image>` and `docker inspect`
- Secure build secrets alternatives (Docker BuildKit `--mount=type=secret`)

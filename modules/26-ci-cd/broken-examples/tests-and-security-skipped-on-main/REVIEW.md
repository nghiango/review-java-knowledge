# Code Review: CI & Container Security Pipeline

## Pull Request Description
This PR optimizes our GitHub Actions CI pipeline to reduce build and deployment times on the `main` branch.
Key changes:
- Skipped test execution during `bootJar` packaging (`-x test`) on `main` pushes under the rationale that tests already ran when the pull request was merged.
- Added Trivy container image scanning for `CRITICAL` and `HIGH` CVEs, but configured `continue-on-error: true` so CVE scanner findings don't block urgent production deployments.

The author notes pipeline execution time dropped from 8 minutes to under 2 minutes.

## Files Under Review
- `ci-pipeline.yml` — GitHub Actions workflow.

## Review Questions
1. Why is skipping tests on `main` dangerous even if tests passed on a feature branch? (Consider merge commits, fast-forwards, dependency bumps, and environment drift).
2. What are the legal, regulatory, and security risks of `continue-on-error: true` on container vulnerability scanners?
3. How should CI pipelines be structured with parallelization, test caching, and strict quality gates to achieve both speed and uncompromising quality?

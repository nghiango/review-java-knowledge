# Solution: Automated Production Release Pipeline

## Annotated Target

```yaml
name: Production Release Pipeline

on:
  push:
    branches: [ main ]

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout Code
        uses: actions/checkout@v4

      # Reliability issue: Mutable image tagging (:latest) prevents deterministic deployments and makes rollback impossible.
      - name: Build Docker Image
        run: |
          docker build -t 123456789012.dkr.ecr.us-east-1.amazonaws.com/order-service:latest .

      # Reliability issue: Overwriting :latest in registry erases the previous working image, preventing fast container rollback.
      - name: Push Docker Image to ECR
        run: |
          docker push 123456789012.dkr.ecr.us-east-1.amazonaws.com/order-service:latest

      # Reliability issue: Triggering deployment without capturing previous task definition revision prevents automated rollback on failure.
      # Reliability issue: Missing post-deployment health validation, smoke tests, and automated rollback triggers.
      - name: Trigger In-Place Deployment
        run: |
          aws ecs update-service \
            --cluster prod-cluster \
            --service order-service \
            --force-new-deployment
```

---

## Discovered Issues

### Reliability issue: Mutable `:latest` Image Tagging
Tagging container images with `:latest` causes severe operational nondeterminism:
1. **Loss of Immutability**: Overwriting `:latest` destroys the reference to the previously running, stable image. If a rollback is needed immediately, there is no discrete `:v1.2.3` or git SHA tag to revert to.
2. **Autoscaling Inconsistency**: If an ECS task autoscales horizontally 2 hours later, newly spawned tasks pull the newest `:latest` image while existing tasks run an older binary, creating a split-brain environment where different containers run different application code versions.
3. **Docker Layer Cache Skew**: Nodes caching an earlier `:latest` image will not pull updated layers unless `--pull` is explicitly enforced.

### Reliability issue: Missing Health Gates and Automated Rollback
The pipeline issues `aws ecs update-service` and exits immediately:
1. It does not wait for the deployment to stabilize (`aws ecs wait services-stable`).
2. It does not execute automated post-deployment synthetic smoke tests against `/actuator/health/readiness`.
3. If new tasks fail to start (e.g. `CrashLoopBackOff` or `OutOfMemoryError`), the pipeline reports **success (green)** while production customers experience complete service degradation!
4. There is no automated rollback trigger to revert the ECS service to the previous known-good task definition revision.

---

## Correct Implementation

See [`correct/release.yml`](correct/release.yml) for the production-grade CD pipeline featuring immutable Git SHA tagging, AWS ECS task definition registration, deployment stability verification, and automated rollback triggers.

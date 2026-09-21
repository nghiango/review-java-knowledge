# Code Review: Automated Production Release Pipeline

## Pull Request Description
This PR introduces our continuous deployment pipeline for the Order Fulfillment service deployed to Amazon ECS on AWS Fargate.
Pipeline features:
- Automatically builds container image on push to `main`.
- Tags image with `latest` and pushes to private Amazon ECR.
- Calls `aws ecs update-service --force-new-deployment` to trigger an instant rolling update.

The author claims this eliminates deployment complexity by ensuring all tasks always pull the latest build.

## Files Under Review
- `release.yml` — GitHub Actions production deployment workflow.

## Review Questions
1. Why is tagging container images with `:latest` in production an anti-pattern? What happens during autoscaling or rollback events?
2. What happens if the new container version experiences an unhandled startup crash (`ApplicationContextException`)? How does this pipeline detect and handle failure?
3. How should automated smoke tests, immutable version tagging, and automated rollback triggers be incorporated into an enterprise continuous delivery pipeline?

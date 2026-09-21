# Code Review: Unbounded Retry Storm Without Jitter

## Context
A financial services payment integration client retries failed downstream payment calls. Review the retry coordination strategy for production readiness under high-concurrency virtual threads.

## Review Questions
1. Is there an upper bound on retry attempts, or does this risk infinite execution?
2. What happens to the downstream service when thousands of concurrent virtual threads fail simultaneously and retry at identical 100ms intervals?
3. How should backoff be calculated to avoid synchronized thundering herd retry storms?
4. Which exception types should trigger retries (e.g. transient network errors vs non-retryable 4xx client rejections)?

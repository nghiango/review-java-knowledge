# Code Review Target: Wrong HTTP Status Codes (Envelope 200 Anti-Pattern)

Review the following REST controller and response envelope. Identify violations of REST architectural style and HTTP semantics.

## Files Under Review

- `ApiResponse.java`
- `Product.java`
- `ProductController.java`

## Review Objectives

1. Determine what HTTP status code is returned when a product is not found or validation fails.
2. Evaluate how API gateways, CDN caches, and HTTP clients interpret `200 OK` errors.
3. Check HTTP response semantics for resource creation (`POST`).

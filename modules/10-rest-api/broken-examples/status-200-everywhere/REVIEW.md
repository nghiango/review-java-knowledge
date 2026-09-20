# Code Review: Status 200 Everywhere

## Context
A product catalog API returns HTTP `200 OK` for all operations, encoding creation, deletion, and missing resource errors within the response JSON payload.

## Code Under Review
- `ProductManagementController.java` — REST controller handling creation, retrieval, and deletion of products.

## Review Questions
1. Why is returning `200 OK` with `{ "status": "ERROR", "code": 404 }` an anti-pattern for REST clients and infrastructure intermediaries?
2. What HTTP status code and response header should be returned when a resource is successfully created via `POST`?
3. What is the semantic status code for successful deletions (`204 No Content`)?

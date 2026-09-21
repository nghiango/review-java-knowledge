# Code Review: Superficial HTTP Status Assertions & Brittle JSON Testing

## Context
A test suite is submitted to test `CustomerOrderApiController`. The author tests the invalid ID error pathway and happy path response.

## Files Under Review
- `CustomerOrderApiController.java`
- `LegacyOrderEndpointTest.java`

## Review Questions
1. In `testInvalidOrderReturnsError`, what happens if the controller changes the error response schema, alters the error title, or removes the error code property? Would this test detect the regression?
2. In `testValidOrderReturnsJsonString`, what happens if Jackson serializes properties in a slightly different order, or adds an optional field? Why is strict string equality brittle?
3. How does Spring Framework 7 `RestTestClient` provide fluent, type-safe assertions for RFC 9457 `ProblemDetail` responses and record payloads?

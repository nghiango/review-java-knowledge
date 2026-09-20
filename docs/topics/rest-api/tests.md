# REST API Unit & Contract Tests

This page documents the test suites validating HTTP method safety, status codes, RFC 9457 Problem Details, idempotency replays, pagination bounds, and optimistic concurrency.

---

## 1. Safe Order Controller Tests

Verifies that `GET` endpoints are read-only and `POST` mutations behave correctly.

```java
--8<-- "modules/10-rest-api/src/test/java/lab/restapi/safemethods/SafeOrderControllerTest.java"
```

---

## 2. Safe Product Controller Tests

Verifies standard HTTP status code selection: `201 Created` with `Location`, `200 OK`, `204 No Content`, and `404 Not Found`.

```java
--8<-- "modules/10-rest-api/src/test/java/lab/restapi/statuscodes/SafeProductControllerTest.java"
```

---

## 3. Idempotent Payment Controller Tests

Verifies `Idempotency-Key` tracking, fresh processing (`201 Created`), and cached replay (`200 OK` + `Idempotency-Replayed: true`).

```java
--8<-- "modules/10-rest-api/src/test/java/lab/restapi/idempotency/IdempotentPaymentControllerTest.java"
```

---

## 4. Safe User Admin Controller Tests

Verifies that sensitive fields (`passwordHash`, `role`) are protected against leakage and mass assignment.

```java
--8<-- "modules/10-rest-api/src/test/java/lab/restapi/dtosecurity/SafeUserAdminControllerTest.java"
```

---

## 5. Problem Detail Exception Handler Tests

Verifies RFC 9457 `ProblemDetail` generation for `404 Not Found`, `422 Unprocessable Entity`, and validation errors.

```java
--8<-- "modules/10-rest-api/src/test/java/lab/restapi/problemdetails/ProblemDetailRestExceptionHandlerTest.java"
```

---

## 6. Safe Catalog Controller Tests

Verifies pagination bounding, page calculations, and protection against excessive limit parameters.

```java
--8<-- "modules/10-rest-api/src/test/java/lab/restapi/pagination/SafeCatalogControllerTest.java"
```

---

## 7. Safe Document Controller Tests

Verifies `ETag` generation, `304 Not Modified` conditional GETs, `428 Precondition Required`, and `412 Precondition Failed` on stale concurrency headers.

```java
--8<-- "modules/10-rest-api/src/test/java/lab/restapi/concurrency/SafeDocumentControllerTest.java"
```

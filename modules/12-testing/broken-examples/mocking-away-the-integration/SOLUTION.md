# Solution: Order fulfilment test that mocks away the integration

## Annotated code

### `InventoryClient.java`

```java
package lab.testing.broken.mockingawaytheintegration;

import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

/** Looks up stock levels from the inventory service. */
public final class InventoryClient {

    private final RestClient restClient;
    private final String baseUrl;

    public InventoryClient(RestClient restClient, String baseUrl) {
        this.restClient = restClient;
        this.baseUrl = baseUrl;
    }

    // Design issue: The method returns the raw JSON body as an untyped Map<String, Object>, so
    // every caller has to know the wire field names and cast the values itself. The transport
    // format leaks into the domain layer, and a renamed field surfaces as a NullPointerException
    // or ClassCastException far from the client that should own the contract.
    public Map<String, Object> get(String sku) {
        return restClient
                .get()
                // Testing issue: This endpoint is never exercised, because the test doubles
                // InventoryClient. The inventory API serves GET /inventory/{sku}; this client
                // calls GET /stock/{sku}, so every real lookup returns 404 and no test notices.
                .uri(baseUrl + "/stock/{sku}", sku)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});
    }
}
```

### `OrderFulfilmentService.java`

```java
package lab.testing.broken.mockingawaytheintegration;

import java.util.Map;

/** Decides whether an order can be fulfilled from current inventory. */
public final class OrderFulfilmentService {

    private final InventoryClient inventoryClient;

    public OrderFulfilmentService(InventoryClient inventoryClient) {
        this.inventoryClient = inventoryClient;
    }

    public boolean canFulfil(String sku, int quantity) {
        Map<String, Object> body = inventoryClient.get(sku);
        // Testing issue: The field name is taken from the test's stub, not from the API. The
        // inventory response uses "available"; the test fabricates a map keyed "quantity", so this
        // line is never compared with the real payload and throws a NullPointerException in
        // production while the suite stays green.
        int available = ((Number) body.get("quantity")).intValue();
        return available >= quantity;
    }
}
```

### `OrderFulfilmentServiceTest.java`

```java
package lab.testing.broken.mockingawaytheintegration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderFulfilmentServiceTest {

    // Testing issue: InventoryClient is the HTTP boundary itself, so doubling it deletes the only
    // thing a contract test should check. No request is ever sent, so the wrong path (/stock/) and
    // the wrong JSON field ("quantity") cannot fail the suite — it proves only that a stub was
    // called.
    @Mock private InventoryClient inventoryClient;

    // Reliability issue: Only the happy path is covered. Nothing exercises a slow inventory
    // service, a 5xx response or a malformed body, and no connect or read timeout is configured on
    // the real RestClient, so a single stalled call would block the request thread.
    @Test
    void canFulfil_whenStockIsSufficient_returnsTrue() {
        // Testing issue: The stub hands the service a hand-built Map, so the test never asserts the
        // request path, the Accept header or that the JSON body deserializes into a typed response.
        // A change to the inventory API's URL or payload cannot be observed here.
        when(inventoryClient.get("A-1"))
                .thenReturn(Map.<String, Object>of("sku", "A-1", "quantity", 3));

        boolean result = new OrderFulfilmentService(inventoryClient).canFulfil("A-1", 3);

        assertTrue(result);
    }

    @Test
    void canFulfil_whenStockIsInsufficient_returnsFalse() {
        when(inventoryClient.get("A-1"))
                .thenReturn(Map.<String, Object>of("sku", "A-1", "quantity", 3));

        boolean result = new OrderFulfilmentService(inventoryClient).canFulfil("A-1", 4);

        assertFalse(result);
    }
}
```

## Issues

| # | Category | Severity | Location | Summary |
|---|---|---|---|---|
| 1 | Testing issue | High | `OrderFulfilmentServiceTest` (`@Mock InventoryClient`) | Doubling the HTTP boundary hides contract drift: the client's `/stock/` path and the `"quantity"` field both ship unnoticed |
| 2 | Testing issue | Medium | `OrderFulfilmentServiceTest.canFulfil_*()` | No assertion on the request path, `Accept` header or JSON deserialization |
| 3 | Reliability issue | High | `OrderFulfilmentServiceTest` / `InventoryClient` | Happy-path-only coverage and no connect or read timeout on the outbound call |
| 4 | Design issue | Medium | `InventoryClient.get()` | Returns an untyped `Map`, leaking the wire format into the domain |

## Issue details

### Mocking the HTTP boundary hides contract drift

**Type:** Testing issue · **Severity:** High · **Difficulty:** Intermediate
**Technology:** Mockito, WireMock · **Interview frequency:** High · **Production impact:** High

**Location:** `OrderFulfilmentServiceTest` (`@Mock InventoryClient`)

#### Problem
The test replaces `InventoryClient` — the outbound HTTP boundary — with a mock, so no request is ever
built or sent. The client's path (`/stock/{sku}` instead of `/inventory/{sku}`) and the JSON field the
service reads (`"quantity"` instead of `"available"`) are therefore never compared with the real
inventory API, and both defects ship with a green suite.

#### Why it happens
Mocking the collaborator that performs the I/O is the fastest way to make a service method testable,
and the resulting test "passes" — but it verifies the service's arithmetic against a payload the test
itself invented. The one thing that can drift silently (the wire contract) is exactly the thing the
mock removes.

#### Production impact
```text
Inventory API renamed GET /inventory/{sku} -> path mismatch is never tested
→ client calls GET /stock/A-1 → 404 Not Found → RestClient throws
→ every canFulfil call fails; the mocked suite stayed green through the whole rollout
```

#### Broken implementation
```java
@Mock private InventoryClient inventoryClient;
...
when(inventoryClient.get("A-1"))
        .thenReturn(Map.<String, Object>of("sku", "A-1", "quantity", 3));
```

#### Correct implementation
```java
// src/integrationTest/java/lab/testing/fulfilment/OrderFulfilmentWireMockIT.java
inventoryServer.stubFor(
        get(urlEqualTo("/inventory/A-1"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"sku\":\"A-1\",\"available\":3}")));

assertThat(service.canFulfil("A-1", 3)).isTrue();
```

#### Why the solution works
A stub server speaks the real protocol, so the client's request path, header and body binding are all
exercised. If the inventory API renames its endpoint or a field, the contract test fails instead of
the mocks staying green.

#### Trade-offs
A stub server is slower and heavier than a mock and needs a port and lifecycle management, so it is
reserved for the boundary. Keep the client thin (build the request, bind the response) and unit-test
the surrounding domain logic with a fake client.

#### How to detect it
Look for a `@Mock` on a class whose name ends in `Client`, `Gateway`, `Repository` or `Api` and whose
method performs I/O. Ask: "if the remote contract changed, which test would fail?" If the answer is
"none", the boundary is mocked away.

#### Interview follow-up
> Where is the line between a unit test that should mock a collaborator and a contract test that must
> not? How does that line move for a client that only transforms data?

#### Related
- Test doubles · Contract testing · WireMock · Mock only at the architectural boundary

### No assertion on the wire contract

**Type:** Testing issue · **Severity:** Medium · **Difficulty:** Intermediate
**Technology:** WireMock, Spring `RestClient` · **Interview frequency:** Medium · **Production impact:** Medium

**Location:** `OrderFulfilmentServiceTest.canFulfil_whenStockIsSufficient_returnsTrue()`

#### Problem
Even if the client were used for real, this test asserts nothing about the request it produces: not
the URL path, not the `Accept` header, not that the response body deserializes into a typed object.
The stub hands the service a `Map` it built itself, so the serialization contract is entirely absent.

#### Why it happens
The test was written to check a branch (`available >= quantity`) and stopped there. Verifying the
outbound request feels like testing the framework, so the HTTP-level contract is left implicit.

#### Production impact
```text
Accept header dropped or set to text/plain
→ inventory API returns 406 or a different representation
→ client deserialization fails in production; no test covered the request shape
```

#### Broken implementation
```java
when(inventoryClient.get("A-1"))
        .thenReturn(Map.<String, Object>of("sku", "A-1", "quantity", 3));
```

#### Correct implementation
```java
inventoryServer.verify(
        getRequestedFor(urlEqualTo("/inventory/A-1"))
                .withHeader("Accept", equalTo(MediaType.APPLICATION_JSON_VALUE)));
```

#### Why the solution works
`WireMock` records the requests the client actually sent, so the path and the `Accept` header become
assertions. The stubbed JSON is bound by Jackson into `InventoryResponse`, so a field rename fails the
test instead of silently producing `null`.

#### Trade-offs
Asserting the exact path couples the test to the API version, which is the point for a contract test —
but assert the fields that are genuinely part of the contract and avoid pinning headers the API
ignores.

#### How to detect it
Search a mocked-client test for any assertion on the request object (URL, headers, body). If there is
none, the wire contract is untested regardless of how many branches are covered.

#### Interview follow-up
> Which parts of an HTTP request belong in a contract test, and which are implementation details the
> test should not pin down?

#### Related
- Contract testing · WireMock request verification · Jackson deserialization · HTTP semantics

### No timeout or error-path coverage on an outbound call

**Type:** Reliability issue · **Severity:** High · **Difficulty:** Intermediate
**Technology:** Spring `RestClient` · **Interview frequency:** High · **Production impact:** High

**Location:** `OrderFulfilmentServiceTest` / `InventoryClient`

#### Problem
The outbound call is built with no connect or read timeout, and the suite covers only the happy path.
A slow or failing inventory service is never exercised: a hung connection blocks the calling thread,
and a 5xx or unparseable body surfaces as a raw `RestClientException` (or `NullPointerException`)
rather than a handled failure.

#### Why it happens
The default `RestClient` inherits the underlying HTTP client's (or the container's) defaults, which
are effectively unbounded for a synchronous call. Because the client is mocked, the test never touches
the transport and the missing policy is invisible.

#### Production impact
```text
inventory service degrades (p99 latency 30s)
→ every fulfilment request holds its thread waiting on the socket
→ Tomcat thread pool saturates → the whole application stops serving, not just fulfilment
```

#### Broken implementation
```java
@Mock private InventoryClient inventoryClient;   // transport never exercised
// no setConnectTimeout / setReadTimeout anywhere in the module
```

#### Correct implementation
```java
SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
requestFactory.setConnectTimeout(Duration.ofSeconds(2));
requestFactory.setReadTimeout(Duration.ofSeconds(2));
RestClient restClient =
        RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
```

#### Why the solution works
An explicit timeout bounds the worst-case latency of the call, so a stalled inventory service fails
fast and releases the thread. Because the contract test uses a real `RestClient`, the timeout policy
is part of the tested configuration; a separate test can stub a delayed or 5xx response to cover the
error path.

#### Trade-offs
A timeout that is too short turns a slow-but-correct dependency into spurious failures; choose it from
the dependency's SLO and add a bounded retry with jitter for transient failures rather than lengthening
the timeout.

#### How to detect it
Grep for `RestClient`, `WebClient` or `HttpClient` construction without a request factory or timeout
setting. In tests, check that at least one case covers a timeout, a 5xx and a malformed body.

#### Interview follow-up
> How would you test that a timeout is actually applied, and how do you avoid a retry storm once a
> dependency starts timing out?

#### Related
- Explicit timeouts · Bounded retries · Resilience4j · Failure-path testing

### HTTP client interface leaks the wire format

**Type:** Design issue · **Severity:** Medium · **Difficulty:** Intermediate
**Technology:** Spring `RestClient`, Jackson · **Interview frequency:** Medium · **Production impact:** Medium

**Location:** `InventoryClient.get()`

#### Problem
`get` returns `Map<String, Object>`, so the caller must know the JSON field names and cast the values
itself (`((Number) body.get("quantity")).intValue()`). The transport format is not encapsulated: a
renamed or retyped field ripples through the domain layer and fails as a `NullPointerException` far
from the client.

#### Why it happens
Returning a `Map` avoids writing a DTO and "just works" while the payload is small. It also moves the
deserialization decision out of the client, which is the one place that should own it.

#### Production impact
```text
API changes "available" from a number to a string ("3")
→ ((Number) body.get("quantity")) throws ClassCastException
→ the failure is attributed to OrderFulfilmentService, not to the client that should adapt
```

#### Broken implementation
```java
public Map<String, Object> get(String sku) {
    return restClient.get()
            .uri(baseUrl + "/stock/{sku}", sku)
            .retrieve()
            .body(new ParameterizedTypeReference<Map<String, Object>>() {});
}
```

#### Correct implementation
```java
public InventoryResponse lookup(String sku) {
    return client.get()
            .uri(baseUrl + "/inventory/{sku}", sku)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(InventoryResponse.class);
}
```

#### Why the solution works
`InventoryResponse` is a typed record, so Jackson binds the payload once, inside the client, and the
domain is written against `response.available()`. A wire-format change is a single-file change and a
`null`/type mismatch fails at the boundary that owns the contract.

#### Trade-offs
A DTO per response is more code and couples the domain to the API's shape; where the payload is
genuinely dynamic, return a documented type rather than a raw `Map`, and consider a dedicated
anti-corruption layer if the remote model differs from the domain model.

#### How to detect it
Any client or repository method whose return type is `Map<String, Object>`, `JsonNode` or `String`
(the raw body) is leaking the wire format. Ask: "how many call sites would change if a field is
renamed?"

#### Interview follow-up
> When, if ever, is returning an untyped map from a client justified, and how would you keep the wire
> format from spreading into the domain?

#### Related
- Anti-corruption layer · DTOs vs domain models · Jackson binding · Encapsulation

## Correct implementation

The production-ready counterpart lives in `lab.testing.fulfilment`:

- [`InventoryResponse.java`](../../src/main/java/lab/testing/fulfilment/InventoryResponse.java) — typed
  record the client binds the JSON body into.
- [`InventoryClient.java`](../../src/main/java/lab/testing/fulfilment/InventoryClient.java) — calls
  `GET /inventory/{sku}`, declares `Accept: application/json` and returns the typed response; the
  transport is an injected `RestClient` that carries the timeout policy.
- [`OrderFulfilmentService.java`](../../src/main/java/lab/testing/fulfilment/OrderFulfilmentService.java)
  — compares the requested quantity against `InventoryResponse.available()`, never a wire field name.
- [`OrderFulfilmentWireMockIT.java`](../../src/integrationTest/java/lab/testing/fulfilment/OrderFulfilmentWireMockIT.java)
  — contract test against a WireMock stub server: asserts `canFulfil("A-1", 3)` is true and
  `canFulfil("A-1", 4)` is false, verifies the recorded request path and `Accept` header, and uses a
  real `RestClient` with explicit connect and read timeouts.

Walkthrough and trade-offs: [Testing — solutions](../../../../docs/topics/testing/solutions.md).

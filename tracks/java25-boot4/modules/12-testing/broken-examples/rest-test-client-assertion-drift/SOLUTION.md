# Solution: Superficial HTTP Status Assertions & Brittle JSON Testing

## Annotated Code

```java
package lab.java25boot4.testing.broken.assertiondrift;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

public class LegacyOrderEndpointTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new CustomerOrderApiController()).build();
    }

    @Test
    public void testInvalidOrderReturnsError() throws Exception {
        // Maintainability issue: asserting HTTP status alone without verifying RFC 9457 ProblemDetail attributes masks response contract regressions
        mockMvc.perform(get("/api/customer-orders/invalid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testValidOrderReturnsJsonString() throws Exception {
        // Reliability issue: brittle raw JSON string assertions break on harmless property reordering while ignoring RFC 8594 lifecycle headers
        String responseBody = mockMvc.perform(get("/api/customer-orders/ORD-123"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseBody).isEqualTo("{\"id\":\"ORD-123\",\"amountCents\":4500,\"status\":\"COMPLETED\"}");
    }
}
```

---

## Issues Identified

### 1. Superficial HTTP Status Code Assertion Masking Contract Regressions
- **Category:** Maintainability
- **Track:** `java25-boot4`
- **Severity:** High
- **Description:** Checking only `status().isBadRequest()` verifies that an error occurred, but does not verify whether the error payload conforms to RFC 9457 `ProblemDetail` (title, detail, type, extension properties). Downstream clients relying on specific problem fields will break undetected if the error structure is inadvertently changed or replaced by an empty body.
- **Remediation:** Assert the complete RFC 9457 error structure using `RestTestClient` or `jsonPath` to verify `title`, `detail`, and custom extension properties.

### 2. Brittle JSON String Equality Comparison
- **Category:** Reliability
- **Track:** `java25-boot4`
- **Severity:** Medium
- **Description:** Using raw string equality (`assertThat(responseBody).isEqualTo(...)`) fails when Jackson changes field order, formats numbers differently, or omits null fields. Furthermore, it completely overlooks HTTP response headers (such as `Content-Type`, `Cache-Control`, or RFC 8594 `Sunset` headers).
- **Remediation:** Use `RestTestClient` with typed DTO deserialization (`expectBody(OrderRecord.class).isEqualTo(...)`) or JsonPath assertions that ignore irrelevant whitespace and property ordering.

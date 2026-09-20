# Solution — Logging Secrets and PII

## Annotated code

```java
package lab.observability.broken.loggingsecrets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UserAuthenticationLogger {

    private static final Logger log = LoggerFactory.getLogger(UserAuthenticationLogger.class);

    public void logLoginAttempt(String username, String password, String clientIp) {
        // Security issue: Logging plaintext password exposes customer credentials to log shippers and indexing stores.
        // Observability issue: String concatenation bypasses SLF4J parameterized logging and risks log injection.
        log.info("User login attempt: username=" + username + ", password=" + password + ", ip=" + clientIp);
    }

    public void logSessionCreated(String username, String bearerToken) {
        // Security issue: Logging bearer authentication tokens permits unauthorized session hijacking via log access.
        log.info("User authenticated: " + username + " with token=" + bearerToken);
    }

    public void logPaymentProcessed(String orderId, String cardNumber, String cvv, double amount) {
        // Security issue: Storing full credit card primary account numbers (PAN) and CVVs violates PCI-DSS requirements.
        log.debug("Processed payment for order " + orderId + " using card " + cardNumber + " cvv=" + cvv + " amount=" + amount);
    }
}
```

## Issue list

### Security issue: Plaintext passwords and bearer tokens logged in clear text

- **Location:** `UserAuthenticationLogger.java:15`
- **Description:** Logging raw credentials (`password`, `bearerToken`) writes sensitive authentication secrets to log files, centralized aggregators (Elasticsearch/Datadog), and developer consoles.
- **Impact:** Anyone with read access to log aggregators or SIEM tools can harvest credentials and impersonate users across corporate systems.
- **Remediation:** Never log passwords or full authentication tokens. Log only the user identifier and outcome, or hash/truncate token identifiers.

### Security issue: Unmasked credit card PAN and CVV logged in violation of PCI-DSS

- **Location:** `UserAuthenticationLogger.java:23`
- **Description:** Logging full 16-digit credit card numbers and 3-digit CVVs directly violates PCI-DSS Requirement 3.2.
- **Impact:** Severe regulatory fines, loss of payment processing merchant privileges, and extreme vulnerability during data breaches.
- **Remediation:** Mask primary account numbers (e.g. `****-****-****-1234`) and completely eliminate CVV variables from logging scopes.

### Observability issue: String concatenation instead of parameterized logging

- **Location:** `UserAuthenticationLogger.java:15`
- **Description:** Using `+` concatenation builds strings eagerly even when the log level is disabled, and permits log injection attacks via unescaped CRLF characters.
- **Impact:** CPU overhead and garbage collection churn; vulnerability to log forging.
- **Remediation:** Use SLF4J parameterized placeholders (`log.info("User login attempt username={} ip={}", username, clientIp)`).

## Correct implementation

See [`lab.observability.loggingsecrets.CorrectUserAuthenticationLogger`](../../src/main/java/lab/observability/loggingsecrets/CorrectUserAuthenticationLogger.java).

Detailed discussion in [Solutions](../../../docs/topics/observability/solutions.md#1-sanitizing-logs-and-masking-sensitive-data).

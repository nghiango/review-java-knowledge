# Solution — Blocking JDBC in WebFlux

## Annotated code

```java
package lab.webflux.broken.blockingjdbc;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class CustomerSummaryService {

  private final JdbcClient jdbcClient;

  public CustomerSummaryService(JdbcClient jdbcClient) {
    this.jdbcClient = jdbcClient;
  }

  public Mono<CustomerSummary> getCustomerSummary(String customerId) {
    // Concurrency issue: Blocking JDBC query executed synchronously on the subscriber thread.
    // By default, Mono.fromSupplier executes on the thread requesting the item (the Netty event loop).
    // Traditional JDBC drivers block on socket reads for query results and connection pool acquisition,
    // freezing the reactive event loop and causing severe latency spikes across all concurrent requests.
    // Performance issue: Missing subscribeOn(Schedulers.boundedElastic()) to offload blocking I/O.
    return Mono.fromSupplier(() -> {
      return jdbcClient.sql("SELECT customer_id, name, balance FROM customers WHERE customer_id = :id")
          .param("id", customerId)
          .query(CustomerSummary.class)
          .single();
    });
  }

  public record CustomerSummary(String customerId, String name, double balance) {}
}
```

## Issue list

### Concurrency issue: Synchronous blocking JDBC executed on reactive Netty event loop

- **Location:** `CustomerSummaryService.java:18`
- **Description:** `Mono.fromSupplier(...)` executes the supplier lambda on the subscribing thread. If subscribed by a Netty HTTP handler, the blocking JDBC network call runs directly on `reactor-http-epoll`.
- **Impact:** Slow database queries or HikariCP connection pool waits block the Netty event loop, freezing hundreds of other non-database HTTP requests handled by the same core.
- **Remediation:** Offload blocking database operations to a dedicated thread pool using `.subscribeOn(Schedulers.boundedElastic())` or migrate to a non-blocking reactive driver using R2DBC.

### Performance issue: Unbounded execution risking event loop starvation

- **Location:** `CustomerSummaryService.java:18`
- **Description:** No scheduler isolation or execution timeout is attached to the blocking supplier.
- **Impact:** Long-running database locks or network partitions between the app and database cause permanent thread stalls.
- **Remediation:** Explicitly chain `.subscribeOn(Schedulers.boundedElastic())` and specify `.timeout(Duration.ofSeconds(...))`.

## Correct implementation

See [`lab.webflux.blockingjdbc.CorrectCustomerSummaryService`](../../src/main/java/lab/webflux/blockingjdbc/CorrectCustomerSummaryService.java).

Detailed discussion in [Solutions](../../../docs/topics/webclient-webflux/solutions.md#offloading-blocking-jdbc-to-boundedelastic).

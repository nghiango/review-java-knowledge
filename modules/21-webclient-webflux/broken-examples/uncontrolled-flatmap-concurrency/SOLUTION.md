# Solution — Uncontrolled flatMap Concurrency

## Annotated code

```java
package lab.webflux.broken.flatmapconcurrency;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class NotificationBatchSender {

  private final WebClient webClient;

  public NotificationBatchSender(WebClient.Builder webClientBuilder) {
    this.webClient = webClientBuilder.baseUrl("https://push-notification.internal").build();
  }

  public Flux<NotificationResult> sendBatch(List<NotificationMessage> messages) {
    // Scalability issue: flatMap defaults to Queues.SMALL_BUFFER_SIZE (256 concurrent subscribers).
    // When processing a batch of 10,000 notifications, flatMap immediately requests 256 in-flight calls.
    // If messages arrive faster or if the downstream API is slow, connection pool limit (e.g. 500) is rapidly hit,
    // causing pending acquisition queues to fill and requests to fail with PoolAcquireTimeoutException.
    // Resilience issue: Missing explicit concurrency bound (maxConcurrency parameter) tailored to downstream rate limits.
    // Performance issue: Fanning out 256 parallel HTTP requests per batch hammers downstream push notification services.
    return Flux.fromIterable(messages)
        .flatMap(msg -> {
          return webClient.post()
              .uri("/api/notify")
              .bodyValue(msg)
              .retrieve()
              .bodyToMono(NotificationResult.class);
        });
  }

  public record NotificationMessage(String userId, String title, String body) {}
  public record NotificationResult(String userId, boolean delivered) {}
}
```

## Issue list

### Scalability issue: Uncontrolled `flatMap` concurrency oversubscribes outbound connection pool

- **Location:** `NotificationBatchSender.java:18`
- **Description:** `Flux.flatMap(Function)` defaults to 256 concurrent in-flight inner publishers (`Queues.SMALL_BUFFER_SIZE`).
- **Impact:** Submitting multiple batches causes hundreds of concurrent outbound HTTP calls. The Reactor Netty connection pool is exhausted, leading to `io.netty.channel.ConnectTimeoutException` or `PoolAcquireTimeoutException`, while the downstream service experiences sudden load spikes and returns HTTP 429 / 503 errors.
- **Remediation:** Explicitly provide the `concurrency` overload to `flatMap(mapper, maxConcurrency)` (e.g. `flatMap(fn, 16)`) or use `concatMap()` if strict sequential ordering is required.

### Resilience issue: Missing error isolation causes entire batch to fail on a single error

- **Location:** `NotificationBatchSender.java:19`
- **Description:** If a single user notification throws an exception (e.g. 404 or connection drop), the default behavior of `flatMap` cancels the entire outer flux, leaving the rest of the batch unaddressed.
- **Impact:** One failed user message terminates notification delivery for the remaining 9,999 users.
- **Remediation:** Attach `.onErrorResume()` or `.onErrorReturn()` inside the inner publisher to isolate errors per message.

## Correct implementation

See [`lab.webflux.flatmapconcurrency.CorrectNotificationBatchSender`](../../src/main/java/lab/webflux/flatmapconcurrency/CorrectNotificationBatchSender.java).

Detailed discussion in [Solutions](../../../docs/topics/webclient-webflux/solutions.md#bounded-flatmap-concurrency-and-error-isolation).

# Solution — Missing WebClient Timeout

## Annotated code

```java
package lab.webflux.broken.missingtimeout;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class CarrierTrackingClient {

  private final WebClient webClient;

  public CarrierTrackingClient(WebClient.Builder webClientBuilder) {
    // Resilience issue: Default WebClient has unbounded response timeout on Reactor Netty HttpClient.
    // While WebClient is non-blocking, an unresponsive external endpoint that leaves TCP sockets open
    // without returning response bytes ties up connection pool slots and memory buffers indefinitely.
    // Configuration issue: TCP connection timeout is not set, meaning dropped SYN packets wait for the default 30s.
    this.webClient = webClientBuilder.baseUrl("https://carrier.shipping.external").build();
  }

  public Mono<TrackingInfo> trackShipment(String trackingNumber) {
    // Resilience issue: Missing operator-level .timeout(Duration) deadline on the returned Mono.
    // If the remote carrier stalls on TLS handshakes or slow HTTP payload streaming, the subscriber waits forever.
    return webClient.get()
        .uri("/shipments/{id}", trackingNumber)
        .retrieve()
        .bodyToMono(TrackingInfo.class);
  }

  public record TrackingInfo(String trackingNumber, String status, String eta) {}
}
```

## Issue list

### Resilience issue: Missing response timeout on WebClient Netty connector

- **Location:** `CarrierTrackingClient.java:16`
- **Description:** Constructing `WebClient` without configuring a custom `ReactorClientHttpConnector` with `responseTimeout` and `connectTimeout`.
- **Impact:** Downstream carrier outages with half-open TCP connections cause outbound sockets to stay open indefinitely, exhausting connection pools.
- **Remediation:** Configure `HttpClient.create().responseTimeout(Duration.ofSeconds(...)).option(ChannelOption.CONNECT_TIMEOUT_MILLIS, ...)`.

### Resilience issue: Missing stream-level `.timeout()` on reactive invocation

- **Location:** `CarrierTrackingClient.java:23`
- **Description:** No `.timeout(Duration)` operator is chained onto `Mono<TrackingInfo>`.
- **Impact:** Upstream clients waiting for the response are subjected to unbounded wait times if latency spikes.
- **Remediation:** Chain `.timeout(Duration.ofMillis(...))` and provide a fallback via `.onErrorResume(TimeoutException.class, ...)`.

## Correct implementation

See [`lab.webflux.missingtimeout.CorrectCarrierTrackingClient`](../../src/main/java/lab/webflux/missingtimeout/CorrectCarrierTrackingClient.java).

Detailed discussion in [Solutions](../../../docs/topics/webclient-webflux/solutions.md#configuring-webclient-timeouts).

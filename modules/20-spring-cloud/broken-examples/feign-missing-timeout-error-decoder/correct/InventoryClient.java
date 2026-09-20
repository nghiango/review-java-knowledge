package lab.springcloud.broken.feign.correct;

import feign.Request;
import feign.Response;
import feign.RetryableException;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
    name = "inventory-service",
    url = "${inventory.service.url:http://inventory-service}",
    configuration = InventoryClient.InventoryConfig.class,
    fallbackFactory = InventoryClient.InventoryFallbackFactory.class
)
public interface InventoryClient {

  @GetMapping("/api/inventory/{sku}")
  InventoryDto getStock(@PathVariable("sku") String sku);

  @PostMapping("/api/inventory/{sku}/reserve")
  ReservationResponse reserveStock(
      @PathVariable("sku") String sku,
      @RequestHeader("Idempotency-Key") String idempotencyKey,
      @RequestBody ReservationRequest request
  );

  class InventoryConfig {

    @Bean
    public Request.Options requestOptions() {
      // Connect timeout: 500ms, Read timeout: 2000ms
      return new Request.Options(
          500, TimeUnit.MILLISECONDS,
          2000, TimeUnit.MILLISECONDS,
          true
      );
    }

    @Bean
    public Retryer retryer() {
      // Never retry inside Feign blindly; let application-level Resilience4j handle it
      return Retryer.NEVER_RETRY;
    }

    @Bean
    public ErrorDecoder errorDecoder() {
      return new InventoryErrorDecoder();
    }
  }

  class InventoryErrorDecoder implements ErrorDecoder {
    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
      return switch (response.status()) {
        case 404 -> new ItemNotFoundException("Inventory item not found: " + methodKey);
        case 409, 422 -> new InsufficientStockException("Insufficient stock for reservation: " + methodKey);
        case 503, 504 -> new RetryableException(
            response.status(),
            "Downstream inventory service temporarily unavailable",
            response.request().httpMethod(),
            (Long) null,
            response.request()
        );
        default -> defaultDecoder.decode(methodKey, response);
      };
    }
  }

  @Component
  class InventoryFallbackFactory implements FallbackFactory<InventoryClient> {
    @Override
    public InventoryClient create(Throwable cause) {
      return new InventoryClient() {
        @Override
        public InventoryDto getStock(String sku) {
          // Graceful degradation: return 0 available stock or fallback indication
          return new InventoryDto(sku, 0);
        }

        @Override
        public ReservationResponse reserveStock(
            String sku,
            String idempotencyKey,
            ReservationRequest request
        ) {
          // Fail fast on mutating reservations when circuit breaker is open
          throw new ServiceUnavailableException("Inventory reservation unavailable: " + cause.getMessage(), cause);
        }
      };
    }
  }

  record InventoryDto(String sku, int availableQuantity) {}
  record ReservationRequest(String orderId, int quantity) {}
  record ReservationResponse(String reservationId, boolean confirmed) {}

  class ItemNotFoundException extends RuntimeException {
    public ItemNotFoundException(String message) { super(message); }
  }

  class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String message) { super(message); }
  }

  class ServiceUnavailableException extends RuntimeException {
    public ServiceUnavailableException(String message, Throwable cause) { super(message, cause); }
  }
}

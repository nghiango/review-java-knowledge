package lab.springcloud.broken.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import feign.Retryer;

@FeignClient(
    name = "inventory-service",
    url = "${inventory.service.url}",
    configuration = InventoryClient.InventoryConfig.class
)
public interface InventoryClient {

  @GetMapping("/api/inventory/{sku}")
  InventoryDto getStock(@PathVariable("sku") String sku);

  @PostMapping("/api/inventory/{sku}/reserve")
  ReservationResponse reserveStock(
      @PathVariable("sku") String sku,
      @RequestBody ReservationRequest request
  );

  class InventoryConfig {
    @Bean
    public Retryer retryer() {
      return new Retryer.Default(100, 1000, 10);
    }
  }

  record InventoryDto(String sku, int availableQuantity) {}
  record ReservationRequest(String orderId, int quantity) {}
  record ReservationResponse(String reservationId, boolean confirmed) {}
}

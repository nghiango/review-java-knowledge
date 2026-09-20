package lab.springmvc.exceptionhandling;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderProcessingService service;

    public OrderController(OrderProcessingService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    public ResponseEntity<String> getOrder(@PathVariable("id") String id) {
        return ResponseEntity.ok(service.findOrder(id));
    }
}

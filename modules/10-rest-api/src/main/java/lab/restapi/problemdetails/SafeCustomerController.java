package lab.restapi.problemdetails;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
public class SafeCustomerController {

    private final Map<UUID, Customer> customers = new ConcurrentHashMap<>();

    public SafeCustomerController() {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
        customers.put(
                id, new Customer(id, "Alice Smith", "alice@example.com", new BigDecimal("50.00")));
    }

    @GetMapping("/{id}")
    public Customer getCustomer(@PathVariable UUID id) {
        Customer customer = customers.get(id);
        if (customer == null) {
            throw new CustomerNotFoundException(id);
        }
        return customer;
    }

    @PostMapping("/{id}/charge")
    public Customer chargeCustomer(
            @PathVariable UUID id, @Valid @RequestBody ChargeRequest request) {
        Customer customer = customers.get(id);
        if (customer == null) {
            throw new CustomerNotFoundException(id);
        }
        if (customer.creditBalance().compareTo(request.amount()) < 0) {
            throw new InsufficientCreditException(id, customer.creditBalance(), request.amount());
        }
        Customer updated =
                new Customer(
                        customer.id(),
                        customer.name(),
                        customer.email(),
                        customer.creditBalance().subtract(request.amount()));
        customers.put(id, updated);
        return updated;
    }

    public record ChargeRequest(
            @NotNull @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
                    BigDecimal amount) {}
}

package lab.java25boot4.springmvc;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Demonstrates JSpecify null-safety integration in Spring MVC. The package is annotated
 * with @NullMarked, meaning all unannotated types are non-null.
 */
@RestController
@RequestMapping("/api/customers")
public class JSpecifyCustomerController {

    public record CustomerProfile(
            String id, String name, @Nullable String phone, @Nullable String company) {}

    public record UpdateCustomerRequest(@NotBlank String name, @Nullable String phone) {}

    @GetMapping("/{id}")
    public ResponseEntity<CustomerProfile> getCustomer(
            @PathVariable String id,
            @RequestParam(required = false) @Nullable String fallbackCompany) {
        return ResponseEntity.ok(new CustomerProfile(id, "Customer " + id, null, fallbackCompany));
    }

    @PostMapping("/{id}")
    public ResponseEntity<CustomerProfile> updateCustomer(
            @PathVariable String id, @Valid @RequestBody UpdateCustomerRequest request) {
        String company =
                Optional.ofNullable(request.phone()).map(p -> "Corp-" + p).orElse("Default-Corp");
        return ResponseEntity.ok(new CustomerProfile(id, request.name(), request.phone(), company));
    }
}

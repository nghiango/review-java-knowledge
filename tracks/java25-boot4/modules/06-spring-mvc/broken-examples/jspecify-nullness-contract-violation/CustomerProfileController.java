package lab.java25boot4.springmvc.broken.nullness;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
public class CustomerProfileController {

    @GetMapping("/{id}")
    public CustomerProfileResponse getCustomer(
            @PathVariable String id,
            @RequestParam(required = false) String defaultPhone) {
        // When customer has no phone, returns defaultPhone which can be null
        return new CustomerProfileResponse(id, "Customer " + id, defaultPhone);
    }
}

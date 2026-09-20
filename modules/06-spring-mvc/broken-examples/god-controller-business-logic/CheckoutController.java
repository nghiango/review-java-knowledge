package lab.springmvc.broken.controllerseparation;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {

    @PostMapping
    public String checkout(@RequestBody OrderRequest request) {
        if (request.quantity() <= 0) {
            return "Invalid quantity";
        }
        double subtotal = request.quantity() * request.unitPrice();
        double discount = 0.0;
        if (request.quantity() > 10) {
            discount = subtotal * 0.15;
        }
        double total = subtotal - discount;

        String sql =
                "INSERT INTO orders (customer_id, product_id, total) VALUES ('"
                        + request.customerId()
                        + "', '"
                        + request.productId()
                        + "', "
                        + total
                        + ")";

        String paymentStatus = "SUCCESS";
        String notification = "Email sent to customer " + request.customerId();

        return "Order processed: total="
                + total
                + ", payment="
                + paymentStatus
                + ", sql="
                + sql.length()
                + ", notif="
                + notification;
    }
}

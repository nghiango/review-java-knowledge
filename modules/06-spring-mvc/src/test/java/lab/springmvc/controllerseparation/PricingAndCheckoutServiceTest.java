package lab.springmvc.controllerseparation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class PricingAndCheckoutServiceTest {

    private final PricingService pricingService = new PricingService();
    private final PaymentService paymentService = new PaymentService();
    private final CheckoutService checkoutService =
            new CheckoutService(pricingService, paymentService);
    private final CheckoutController controller = new CheckoutController(checkoutService);

    @Test
    @DisplayName("Pricing service calculates 15% discount for quantities greater than 10")
    void pricingService_appliesBulkDiscount() {
        double totalSmall = pricingService.calculateTotal(5, 100.0);
        assertThat(totalSmall).isEqualTo(500.0);

        double totalBulk = pricingService.calculateTotal(20, 100.0);
        // 2000 - 15% = 1700
        assertThat(totalBulk).isEqualTo(1700.0);
    }

    @Test
    @DisplayName("Checkout controller returns confirmed order response")
    void checkout_successfulOrder() {
        OrderRequest request = new OrderRequest("cust-1", "prod-1", 2, 50.0, "CREDIT_CARD");
        ResponseEntity<OrderResponse> response = controller.checkout(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo("CONFIRMED");
        assertThat(response.getBody().totalAmount()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("Invalid payment method throws exception during checkout")
    void checkout_invalidPayment_throwsException() {
        OrderRequest request = new OrderRequest("cust-1", "prod-1", 2, 50.0, "");

        assertThatThrownBy(() -> checkoutService.checkout(request))
                .isInstanceOf(IllegalStateException.class);
    }
}

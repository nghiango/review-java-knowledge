package lab.designpatterns.decorator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DecoratorOrderTest {

    private DefaultOrderQueryService targetService;
    private OrderQueryPort securePipeline;

    @BeforeEach
    void setUp() {
        targetService = new DefaultOrderQueryService();
        securePipeline = SecureOrderQueryPipeline.createSecurePipeline(targetService);
    }

    @AfterEach
    void tearDown() {
        SecurityContext.clear();
    }

    @Test
    @DisplayName("Admin user can retrieve confidential order through secure pipeline")
    void getOrderSummary_adminAccessConfidential_succeeds() {
        SecurityContext.setUser("alice", "ADMIN");

        OrderSummary summary = securePipeline.getOrderSummary("ORD-CONFIDENTIAL-99");

        assertThat(summary).isNotNull();
        assertThat(summary.orderId()).isEqualTo("ORD-CONFIDENTIAL-99");
        assertThat(summary.confidential()).isTrue();
    }

    @Test
    @DisplayName("Guest user cannot access confidential order even if previously cached by Admin")
    void getOrderSummary_guestAccessConfidential_deniedEvenIfCached() {
        // Step 1: Admin reads and populates cache
        SecurityContext.setUser("admin_user", "ADMIN");
        OrderSummary adminResult = securePipeline.getOrderSummary("ORD-CONFIDENTIAL-99");
        assertThat(adminResult).isNotNull();

        // Step 2: Guest user attempts to access the same confidential order
        SecurityContext.setUser("guest_user", "GUEST");

        // The secure pipeline checks authorization first, rejecting before cache lookup
        assertThatThrownBy(() -> securePipeline.getOrderSummary("ORD-CONFIDENTIAL-99"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Access denied to confidential order");
    }

    @Test
    @DisplayName("Non-confidential order is accessible to regular users and properly cached")
    void getOrderSummary_regularOrder_accessibleAndCached() {
        SecurityContext.setUser("bob", "USER");

        OrderSummary firstCall = securePipeline.getOrderSummary("ORD-1");
        OrderSummary secondCall = securePipeline.getOrderSummary("ORD-1");

        assertThat(firstCall).isNotNull();
        assertThat(secondCall).isSameAs(firstCall);
    }
}

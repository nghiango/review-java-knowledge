package lab.testing.broken.mockingawaytheintegration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderFulfilmentServiceTest {

    @Mock private InventoryClient inventoryClient;

    @Test
    void canFulfil_whenStockIsSufficient_returnsTrue() {
        when(inventoryClient.get("A-1"))
                .thenReturn(Map.<String, Object>of("sku", "A-1", "quantity", 3));

        boolean result = new OrderFulfilmentService(inventoryClient).canFulfil("A-1", 3);

        assertTrue(result);
    }

    @Test
    void canFulfil_whenStockIsInsufficient_returnsFalse() {
        when(inventoryClient.get("A-1"))
                .thenReturn(Map.<String, Object>of("sku", "A-1", "quantity", 3));

        boolean result = new OrderFulfilmentService(inventoryClient).canFulfil("A-1", 4);

        assertFalse(result);
    }
}

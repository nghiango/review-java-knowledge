package lab.springtransactions.publicboundary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PublicBoundaryTransactionTest {

    @Test
    @DisplayName("Should successfully reserve stock via public collaborator method")
    void reserveStock_validQuantity_reservesStock() {
        StockReservationCollaborator collaborator = new StockReservationCollaborator();
        InventoryService service = new InventoryService(collaborator);

        service.reserveStock("SKU-990", 5);

        assertThat(collaborator.getReservedStock("SKU-990")).isEqualTo(5);
    }

    @Test
    @DisplayName("Should throw exception when quantity is non-positive")
    void reserveStock_nonPositiveQuantity_throwsException() {
        StockReservationCollaborator collaborator = new StockReservationCollaborator();
        InventoryService service = new InventoryService(collaborator);

        assertThatThrownBy(() -> service.reserveStock("SKU-991", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    @Test
    @DisplayName("Should throw exception when item is out of stock")
    void reserveStock_outOfStock_throwsException() {
        StockReservationCollaborator collaborator = new StockReservationCollaborator();
        InventoryService service = new InventoryService(collaborator);

        assertThatThrownBy(() -> service.reserveStock("OUT_OF_STOCK", 2))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Stock unavailable");
    }
}

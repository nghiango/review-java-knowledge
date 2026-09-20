package lab.distributeddata.inbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SafeInventoryConsumerTest {

    @Test
    @DisplayName("Should process new message and decrement stock on first delivery")
    void processOrderPlaced_firstDelivery() {
        InboxRepository inboxRepository = mock(InboxRepository.class);
        SafeInventoryConsumer.InventoryRepository inventoryRepository =
                mock(SafeInventoryConsumer.InventoryRepository.class);

        when(inboxRepository.tryAcquireLease("evt-100", "inventory-consumer-group"))
                .thenReturn(true);

        SafeInventoryConsumer consumer =
                new SafeInventoryConsumer(inboxRepository, inventoryRepository);
        boolean processed = consumer.processOrderPlaced("evt-100", "SKU-ABC", 2);

        assertThat(processed).isTrue();
        verify(inventoryRepository, times(1)).decrementStock("SKU-ABC", 2);
    }

    @Test
    @DisplayName("Should detect duplicate message in inbox and skip business mutation")
    void processOrderPlaced_duplicateRedelivery() {
        InboxRepository inboxRepository = mock(InboxRepository.class);
        SafeInventoryConsumer.InventoryRepository inventoryRepository =
                mock(SafeInventoryConsumer.InventoryRepository.class);

        when(inboxRepository.tryAcquireLease("evt-100", "inventory-consumer-group"))
                .thenReturn(false);

        SafeInventoryConsumer consumer =
                new SafeInventoryConsumer(inboxRepository, inventoryRepository);
        boolean processed = consumer.processOrderPlaced("evt-100", "SKU-ABC", 2);

        assertThat(processed).isFalse();
        verify(inventoryRepository, never()).decrementStock("SKU-ABC", 2);
    }
}

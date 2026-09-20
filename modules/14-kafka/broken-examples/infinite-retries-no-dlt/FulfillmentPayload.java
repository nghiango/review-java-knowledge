package lab.kafka.broken.infiniteretries;

public record FulfillmentPayload(
        String orderId,
        String sku,
        int quantity,
        String destinationZip) {}

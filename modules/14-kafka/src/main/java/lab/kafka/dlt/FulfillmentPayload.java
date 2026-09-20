package lab.kafka.dlt;

public record FulfillmentPayload(String orderId, String sku, int quantity, String destinationZip) {}

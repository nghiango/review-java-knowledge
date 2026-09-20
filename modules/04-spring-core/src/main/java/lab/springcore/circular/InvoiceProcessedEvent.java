package lab.springcore.circular;

public record InvoiceProcessedEvent(String orderId, boolean success) {}

package lab.rabbitmq.retrydlx;

import java.math.BigDecimal;

public record InvoiceTask(String invoiceId, String taxNumber, BigDecimal amount) {}

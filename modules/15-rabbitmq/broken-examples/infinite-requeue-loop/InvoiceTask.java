package lab.rabbitmq.broken.infiniterequeue;

import java.math.BigDecimal;

public record InvoiceTask(
        String invoiceId,
        String taxNumber,
        BigDecimal amount) {}

package lab.springsecurity.idor;

import java.math.BigDecimal;
import java.util.UUID;

public record Invoice(
        UUID invoiceId, String ownerUsername, BigDecimal amount, String description) {}

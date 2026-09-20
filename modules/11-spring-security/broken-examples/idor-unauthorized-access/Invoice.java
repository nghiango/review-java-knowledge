package lab.springsecurity.broken.idor;

import java.math.BigDecimal;
import java.util.UUID;

public record Invoice(UUID invoiceId, String ownerUsername, BigDecimal amount, String description) {}

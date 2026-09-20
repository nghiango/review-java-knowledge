package lab.restapi.problemdetails;

import java.math.BigDecimal;
import java.util.UUID;

public record Customer(UUID id, String name, String email, BigDecimal creditBalance) {}

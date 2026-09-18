package lab.corejava.streamprocessing;

import java.math.BigDecimal;
import java.util.Currency;

public record OrderPrice(BigDecimal amount, Currency currency) {}

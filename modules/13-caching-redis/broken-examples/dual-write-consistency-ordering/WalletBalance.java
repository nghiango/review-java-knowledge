package lab.cachingredis.broken.dualwrite;

import java.math.BigDecimal;

public record WalletBalance(String accountId, BigDecimal balance) {}

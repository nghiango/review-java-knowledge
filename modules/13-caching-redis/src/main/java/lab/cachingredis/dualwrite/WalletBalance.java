package lab.cachingredis.dualwrite;

import java.math.BigDecimal;

public record WalletBalance(String accountId, BigDecimal balance) {}

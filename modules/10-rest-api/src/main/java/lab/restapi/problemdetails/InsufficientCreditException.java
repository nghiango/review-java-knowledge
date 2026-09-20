package lab.restapi.problemdetails;

import java.math.BigDecimal;
import java.util.UUID;

public class InsufficientCreditException extends RuntimeException {
    private final UUID customerId;
    private final BigDecimal currentBalance;
    private final BigDecimal requiredAmount;

    public InsufficientCreditException(
            UUID customerId, BigDecimal currentBalance, BigDecimal requiredAmount) {
        super(
                "Insufficient credit balance. Current: "
                        + currentBalance
                        + ", required: "
                        + requiredAmount);
        this.customerId = customerId;
        this.currentBalance = currentBalance;
        this.requiredAmount = requiredAmount;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public BigDecimal getRequiredAmount() {
        return requiredAmount;
    }
}

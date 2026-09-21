package lab.designpatterns.chain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OrderValidationContext {

    private final String orderId;
    private final String customerId;
    private final BigDecimal orderAmount;
    private final List<String> errors = new ArrayList<>();

    public OrderValidationContext(String orderId, String customerId, BigDecimal orderAmount) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.orderAmount = orderAmount;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public BigDecimal getOrderAmount() {
        return orderAmount;
    }

    public void addError(String error) {
        this.errors.add(error);
    }

    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    public boolean isValid() {
        return errors.isEmpty();
    }
}

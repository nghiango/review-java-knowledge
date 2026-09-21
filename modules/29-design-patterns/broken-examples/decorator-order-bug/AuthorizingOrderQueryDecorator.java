package lab.designpatterns.broken.decoratororder;

public class AuthorizingOrderQueryDecorator implements OrderQueryService {

    private final OrderQueryService delegate;

    public AuthorizingOrderQueryDecorator(OrderQueryService delegate) {
        this.delegate = delegate;
    }

    @Override
    public OrderSummary getOrderSummary(String orderId) {
        String role = SecurityContext.getRole();
        if (orderId.contains("CONFIDENTIAL") && !"ADMIN".equals(role)) {
            throw new SecurityException("Access denied to confidential order: " + orderId);
        }
        return delegate.getOrderSummary(orderId);
    }
}

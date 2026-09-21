package lab.designpatterns.broken.decoratororder;

public class OrderQueryServiceFactory {

    public static OrderQueryService createService() {
        OrderQueryService target = new DefaultOrderQueryService();
        OrderQueryService authorizing = new AuthorizingOrderQueryDecorator(target);
        return new CachingOrderQueryDecorator(authorizing);
    }
}

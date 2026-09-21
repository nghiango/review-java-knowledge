package lab.designpatterns.decorator;

/**
 * Factory constructing the decorator pipeline in the correct, secure order: Client ->
 * AuthorizingDecorator (OUTER) -> CachingDecorator (MIDDLE) -> DefaultService (INNER)
 *
 * <p>This guarantees that authorization is checked on EVERY request before the cache is consulted,
 * preventing unauthenticated callers from reading cached sensitive data.
 */
public final class SecureOrderQueryPipeline {

    private SecureOrderQueryPipeline() {}

    public static OrderQueryPort createSecurePipeline(OrderQueryPort targetService) {
        // Inner: Caching wraps the target data source
        OrderQueryPort cachingLayer = new CachingOrderQueryDecorator(targetService);
        // Outer: Authorization wraps caching so unauthorized requests are rejected before hitting
        // the cache
        return new AuthorizingOrderQueryDecorator(cachingLayer);
    }
}

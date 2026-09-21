package lab.java25boot4.restapi.questions;

import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * Q09: How do you migrate legacy manual HttpServiceProxyFactory boilerplate from Spring Boot 3 to
 * declarative client configurations in Spring Boot 4?
 */
public class Q09MigrationHttpServiceProxyFactoryToBoot4Example {

    public interface InventoryService {
        boolean isInStock(String sku);
    }

    public static HttpServiceProxyFactory buildLegacyProxyFactory(String baseUrl) {
        RestClient restClient = RestClient.builder().baseUrl(baseUrl).build();
        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        return HttpServiceProxyFactory.builderFor(adapter).build();
    }

    public static void main(String[] args) {
        var factory = buildLegacyProxyFactory("https://inventory.internal");

        boolean isFactoryConstructed = (factory != null);

        System.out.println("Legacy factory created: " + isFactoryConstructed); // true
        System.out.println(
                "Target interface: "
                        + InventoryService.class.getSimpleName()); // "InventoryService"
    }
}

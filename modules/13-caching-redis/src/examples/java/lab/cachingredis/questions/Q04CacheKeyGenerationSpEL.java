package lab.cachingredis.questions;

import java.lang.reflect.Method;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleKeyGenerator;

/** Q04: How are cache keys generated in Spring Cache via SpEL and custom KeyGenerator beans? */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q04CacheKeyGenerationSpEL {

    // Custom KeyGenerator creating compound keys prefixed with tenant context
    static class TenantKeyGenerator implements KeyGenerator {
        @Override
        public Object generate(Object target, Method method, Object... params) {
            String tenantId = (String) params[0];
            String entityId = (String) params[1];
            return "tenant:" + tenantId + ":" + entityId;
        }
    }

    public static void main(String[] args) throws Exception {
        // Default Spring SimpleKeyGenerator behavior:
        // 0 params -> SimpleKey.EMPTY
        // 1 param  -> the parameter itself
        // N params -> SimpleKey containing all parameters
        KeyGenerator defaultGen = new SimpleKeyGenerator();
        Method dummyMethod = Object.class.getMethod("toString");

        Object singleParamKey = defaultGen.generate(new Object(), dummyMethod, "order-42");
        boolean isDirectParam = "order-42".equals(singleParamKey); // true

        KeyGenerator tenantGen = new TenantKeyGenerator();
        Object compoundKey = tenantGen.generate(new Object(), dummyMethod, "tenant-a", "doc-101");
        boolean isNamespaced = "tenant:tenant-a:doc-101".equals(compoundKey); // true
    }
}

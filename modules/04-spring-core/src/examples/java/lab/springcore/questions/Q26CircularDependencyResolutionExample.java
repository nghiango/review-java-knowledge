package lab.springcore.questions;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.ObjectFactory;

@SuppressWarnings("unused")
public final class Q26CircularDependencyResolutionExample {
    private Q26CircularDependencyResolutionExample() {}

    // Spring's 3-level singleton cache in DefaultSingletonBeanRegistry:
    // 1st level: singletonObjects - fully initialized beans (Map<String, Object>)
    // 2nd level: earlySingletonObjects - instantiated beans exposed early, before properties set (Map<String, Object>)
    // 3rd level: singletonFactories - factories capable of creating early references/proxies (Map<String, ObjectFactory<?>>)
    public static class ThreeLevelCacheSimulation {
        private final Map<String, Object> singletonObjects = new HashMap<>();
        private final Map<String, Object> earlySingletonObjects = new HashMap<>();
        private final Map<String, ObjectFactory<?>> singletonFactories = new HashMap<>();

        public void addSingletonFactory(String beanName, ObjectFactory<?> factory) {
            synchronized (singletonObjects) {
                if (!singletonObjects.containsKey(beanName)) {
                    singletonFactories.put(beanName, factory);
                    earlySingletonObjects.remove(beanName);
                }
            }
        }

        public Object getSingleton(String beanName, boolean allowEarlyReference) {
            Object singletonObject = singletonObjects.get(beanName);
            if (singletonObject == null) {
                synchronized (singletonObjects) {
                    singletonObject = earlySingletonObjects.get(beanName);
                    if (singletonObject == null && allowEarlyReference) {
                        ObjectFactory<?> factory = singletonFactories.get(beanName);
                        if (factory != null) {
                            singletonObject = factory.getObject();
                            earlySingletonObjects.put(beanName, singletonObject);
                            singletonFactories.remove(beanName);
                        }
                    }
                }
            }
            return singletonObject;
        }
    }

    public static void main(String[] args) {
        ThreeLevelCacheSimulation cache = new ThreeLevelCacheSimulation();
        // Bean A is instantiated with empty constructor, placed in 3rd level factory cache:
        cache.addSingletonFactory("serviceA", () -> "early-instance-A");

        // Bean B injecting Bean A resolves early reference from 3rd level cache, promoting it to 2nd level:
        Object earlyA = cache.getSingleton("serviceA", true); // "early-instance-A"

        // Constructor injection fails on cycles because instance creation itself requires the dependency!
    }
}

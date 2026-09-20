# Spring Core Exercises

Hands-on exercises to practice Spring IoC architecture, custom post-processors, and proxy mechanics.

## Exercise 1: Build a Custom @Retryable Annotation with BeanPostProcessor

Implement a custom annotation `@AutoRetry(maxAttempts = 3)` and a `BeanPostProcessor` that wraps matching beans in a dynamic proxy that catches exceptions and retries method execution up to `maxAttempts`.

### Requirements
- `@Target(ElementType.METHOD) @Retention(RetentionPolicy.RUNTIME) @interface AutoRetry { int maxAttempts() default 3; }`
- Custom `BeanPostProcessor` inspecting methods in `postProcessAfterInitialization()`.
- Proxied method retries execution up to `maxAttempts` before propagating failure.

??? question "Reveal solution"
    ```java
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface AutoRetry {
        int maxAttempts() default 3;
    }

    @Component
    public class AutoRetryBeanPostProcessor implements BeanPostProcessor {
        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) {
            Class<?> beanClass = bean.getClass();
            boolean hasRetryMethod = Arrays.stream(beanClass.getMethods())
                    .anyMatch(m -> m.isAnnotationPresent(AutoRetry.class));

            if (!hasRetryMethod || beanClass.getInterfaces().length == 0) {
                return bean;
            }

            return Proxy.newProxyInstance(
                    beanClass.getClassLoader(),
                    beanClass.getInterfaces(),
                    (proxy, method, args) -> {
                        Method targetMethod = beanClass.getMethod(method.getName(), method.getParameterTypes());
                        AutoRetry retry = targetMethod.getAnnotation(AutoRetry.class);
                        int maxAttempts = retry != null ? retry.maxAttempts() : 1;

                        int attempt = 0;
                        while (true) {
                            attempt++;
                            try {
                                return targetMethod.invoke(bean, args);
                            } catch (InvocationTargetException e) {
                                if (attempt >= maxAttempts) {
                                    throw e.getCause();
                                }
                            }
                        }
                    });
        }
    }
    ```

---

## Exercise 2: Decouple Circular Dependency using Domain Events

Refactor tightly-coupled `UserService` and `WelcomeEmailSender` into an asynchronous, event-driven model using `ApplicationEventPublisher`.

### Requirements
- Define `UserRegisteredEvent(String userId, String email)`.
- `UserService` creates users and publishes `UserRegisteredEvent` using constructor-injected `ApplicationEventPublisher`.
- `WelcomeEmailSender` subscribes with `@EventListener`.

??? question "Reveal solution"
    ```java
    public record UserRegisteredEvent(String userId, String email) {}

    @Service
    public class UserService {
        private final ApplicationEventPublisher eventPublisher;

        public UserService(ApplicationEventPublisher eventPublisher) {
            this.eventPublisher = Objects.requireNonNull(eventPublisher);
        }

        public void registerUser(String userId, String email) {
            // Save user to database
            eventPublisher.publishEvent(new UserRegisteredEvent(userId, email));
        }
    }

    @Component
    public class WelcomeEmailSender {
        @EventListener
        public void onUserRegistered(UserRegisteredEvent event) {
            // Send email
        }
    }
    ```

## Related

- [Concepts](concepts.md)
- [Solutions](solutions.md)
- [Tests](tests.md)

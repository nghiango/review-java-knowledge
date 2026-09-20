# Testing Spring Boot Applications

Testing strategies for Spring Boot applications, covering unit testing, slice testing with `ApplicationContextRunner`, Bean Validation testing, and Testcontainers `@ServiceConnection`.

---

## 1. Unit & Slice Testing Principles

Spring Boot applications benefit from layered testing:

1. **Fast Unit Tests (`src/test`)**: Test POJOs, business services, and configuration records in isolation without starting a Spring `ApplicationContext`.
2. **Auto-Configuration Slice Tests (`ApplicationContextRunner`)**: Verify conditional bean registration, property binding, and user bean overrides in milliseconds without loading the entire application context.
3. **Integration Tests (`src/integrationTest`)**: Full `@SpringBootTest` tests using Testcontainers and `@ServiceConnection` against real infrastructure (PostgreSQL, Redis, Kafka).

---

## 2. Test Suite Walkthrough

### Actuator Security Verification
Tests that wildcard and sensitive endpoint exposures are rejected while safe endpoints are approved:

```java
--8<-- "modules/05-spring-boot/src/test/java/lab/springboot/actuatorsecurity/ActuatorSecurityConfigTest.java"
```

### Type-Safe Properties & Bean Validation Tests
Validates that invalid property constraints (e.g. negative billing rate, invalid ISO currency, excessive tax rate) are caught by Jakarta Bean Validation:

```java
--8<-- "modules/05-spring-boot/src/test/java/lab/springboot/propsbinding/BillingPropertiesBindingTest.java"
```

### Auto-Configuration Testing with `ApplicationContextRunner`
Validates that auto-configured beans are registered by default and back off when a user bean is defined:

```java
--8<-- "modules/05-spring-boot/src/test/java/lab/springboot/autoconfigoverride/PaymentClientAutoConfigurationTest.java"
```

### Graceful Task Executor Lifecycle Test
Verifies that background tasks are processed and executor configuration includes graceful shutdown parameters:

```java
--8<-- "modules/05-spring-boot/src/test/java/lab/springboot/gracefulshutdown/GracefulShutdownTaskExecutorTest.java"
```

---

## 3. Running the Test Suites

Run unit and slice tests:

```bash
./gradlew :modules:05-spring-boot:test
```

Run broken example compilation verification:

```bash
./gradlew :modules:05-spring-boot:compileBrokenExamples
```

Run interview question example compilation:

```bash
./gradlew :modules:05-spring-boot:compileExamples
```

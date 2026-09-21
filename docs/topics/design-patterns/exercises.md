# Hands-on Design Patterns Exercises

Practical refactoring tasks to master pattern application, decorator pipelines, and strategy registries in Spring Boot.

---

### Exercise 1: Refactor Monolithic Switch into Spring Strategy Registry

**Goal**: Transform a brittle, procedural tax calculator into an extensible, Open/Closed compliant Strategy pattern.

#### Current Naive Code
```java
public class TaxCalculator {
    public BigDecimal calculateTax(String countryCode, BigDecimal amount) {
        switch (countryCode) {
            case "US": return amount.multiply(new BigDecimal("0.07"));
            case "UK": return amount.multiply(new BigDecimal("0.20"));
            case "DE": return amount.multiply(new BigDecimal("0.19"));
            default: throw new IllegalArgumentException("Unsupported country: " + countryCode);
        }
    }
}
```

#### Requirements
1. Define a `TaxStrategy` interface with `String getCountryCode()` and `BigDecimal calculateTax(BigDecimal amount)`.
2. Implement `@Component` beans for `UsTaxStrategy`, `UkTaxStrategy`, and `DeTaxStrategy`.
3. Create a `TaxStrategyFactory` that auto-wires `List<TaxStrategy>` and constructs an immutable lookup map.
4. Verify via unit tests that adding a new country requires zero modifications to existing classes.

??? tip "Solution guidance"
    ```java
    public interface TaxStrategy {
        String getCountryCode();
        BigDecimal calculateTax(BigDecimal amount);
    }

    @Component
    public class UsTaxStrategy implements TaxStrategy {
        @Override public String getCountryCode() { return "US"; }
        @Override public BigDecimal calculateTax(BigDecimal amount) {
            return amount.multiply(new BigDecimal("0.07"));
        }
    }

    @Component
    public class TaxStrategyFactory {
        private final Map<String, TaxStrategy> strategies;

        public TaxStrategyFactory(List<TaxStrategy> strategyList) {
            this.strategies = strategyList.stream()
                .collect(Collectors.toMap(TaxStrategy::getCountryCode, Function.identity()));
        }

        public TaxStrategy getStrategy(String country) {
            TaxStrategy s = strategies.get(country);
            if (s == null) throw new IllegalArgumentException("Unsupported: " + country);
            return s;
        }
    }
    ```

---

### Exercise 2: Build a Securely Ordered Decorator Pipeline

**Goal**: Create a decorated repository where security authorization wraps a caching decorator, preventing unauthorized access to cached records.

#### Requirements
1. Define `DocumentRepository` with `Document getDocument(String docId)`.
2. Implement `AuthorizingDocumentDecorator` that checks if `SecurityContext.getCurrentRole()` is `ADMIN` for confidential documents.
3. Implement `CachingDocumentDecorator` that caches documents by `docId`.
4. Create a factory method composing them in the correct order:
   $$\text{Client} \longrightarrow \text{AuthorizingDecorator} \longrightarrow \text{CachingDecorator} \longrightarrow \text{TargetRepository}$$
5. Write a unit test proving that once an Admin caches a document, an unauthorized Guest is still rejected with a `SecurityException`.

??? tip "Solution guidance"
    ```java
    public class DocumentPipelineFactory {
        public static DocumentRepository createPipeline(DocumentRepository target) {
            DocumentRepository caching = new CachingDocumentDecorator(target);
            return new AuthorizingDocumentDecorator(caching);
        }
    }
    ```

---

### Exercise 3: Composable Specification Engine for Loan Approvals

**Goal**: Implement a composable specification pattern to evaluate whether a borrower qualifies for an instant loan.

#### Requirements
1. Implement a generic `@FunctionalInterface public interface Specification<T>` with `isSatisfiedBy(T item)`, and default methods `and()`, `or()`, and `not()`.
2. Given record `Borrower(int creditScore, BigDecimal annualIncome, boolean hasBankruptcies)`:
   - Rule A: `CreditScoreSpecification`: credit score $\ge 700$.
   - Rule B: `IncomeSpecification`: annual income $\ge \$50,000$.
   - Rule C: `NoBankruptcySpecification`: hasBankruptcies == false.
3. Compose: `eligible = RuleA.and(RuleB).and(RuleC)`.
4. Write unit tests evaluating pass and fail combinations.

??? tip "Solution guidance"
    ```java
    @FunctionalInterface
    public interface Specification<T> {
        boolean isSatisfiedBy(T candidate);

        default Specification<T> and(Specification<T> other) {
            return candidate -> this.isSatisfiedBy(candidate) && other.isSatisfiedBy(candidate);
        }

        default Specification<T> or(Specification<T> other) {
            return candidate -> this.isSatisfiedBy(candidate) || other.isSatisfiedBy(candidate);
        }

        default Specification<T> not() {
            return candidate -> !this.isSatisfiedBy(candidate);
        }
    }
    ```

## Exceptions and resources

An exception is an event that disrupts the normal execution flow of a program.

Java provides two main categories of exceptions:

- Checked exceptions: These are exceptions that are checked at compile time. They are usually used to indicate that an error has occurred that the program should be able to recover from.
- Unchecked exceptions: These are exceptions that are not checked at compile time. They are usually used to indicate that an error has occurred that the program should not be able to recover from.

![Java exception hierarchy](images/java-exception-hierarchy.png)

`Error` generally represents serious conditions that ordinary application code is not expected to recover from. It is not a subclass of `Exception`

### Checked Exceptions

Checked exceptions are exceptions that compiler requires us to catch or declare using throws. They usually represent failures that callers may be able to recover from.

Use checked exceptions when the caller can reasonably be expected to handle a failure as part of normal application behavior.

Examples include reading file, accessing an external resource, or parsing data with a documented recoverable failure.

The caller has two options to handle `checked` exceptions:

- Catch and handle the exception

```java
try {
    // code that may throw checked exception
} catch (Exception e) {
    // handle exception
}
```

- Declare the exception using `throws` in the method signature, that propagates the exception to the caller

```java
public void method() throws Exception {
    // code that may throw checked exception
}
```

### Unchecked Exceptions

Unchecked exceptions inherit from RuntimeException or Error.

Unchecked exceptions are not enforced by the compiler. They usually extend RuntimeException and can represent invalid arguments, illegal states, or application-specific failures.

However, not every unchecked exception is a programming bug. Applications frequently use custom unchecked exceptions to represent business failures.

### Best practices for exception handling

#### Catching Specific Exceptions

Avoid catching overly broad exceptions unless you are implementing an intentional application boundary.

**Bad practice:**

```java
try {
    readFile();
} catch (Exception e) {
    System.out.println("Something went wrong");
}
```

**Better practice:**

```java
try {
    readFile();
} catch (FileNotFoundException e) {
    System.out.println("File not found");
} catch (IOException e) {
    System.out.println("IO error occurred");
}
```

The catch block now handles a known failure type and preserves the exception.

However, if the method has no useful information to add, it is even better to let the exception propagate without catching it.

#### Never Silently Swallow Exceptions

A swallowed exception occurs when a catch block ignore the failure.

**Bad practice:**

```java
try {
    processPayment();
} catch (PaymentException e) {
    // Ignore
}
```

This is dangerous because the caller may assume the payment succeeded.

A better approach is to propagate the exception or return an explicit failure results, depending on the method's contract.

**Better practice:**

```java
try {
    processPayment();
} catch (PaymentException e) {
    throw new OrderProcessingException(
        "Payment processing failed", e
    );
}
```

This allows the higher layer to decide how to respond.

#### Preserve the original cause

When translating an exception into another exception type, preserve the original cause.

**Bad practice:**

```java
try {
    processPayment();
} catch (PaymentException e) {
    throw new OrderProcessingException("Payment processing failed");
}
```

**Better practice:**

```java
try {
    processPayment();
} catch (PaymentException e) {
    throw new OrderProcessingException("Payment processing failed", e);
}
```

Your custom exception should support cause chaining:

```java
public class OrderProcessingException extends Exception {
    public OrderProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

This preserves the original stack trace and makes troubleshooting much easier.

#### Handle exceptions at the appropriate layer

One important production nuance: a method should handle exceptions at the layer where it can meaningfully respond to them. For example, a data access method should not catch database exceptions and convert them to business exceptions, as it does not know how the caller should respond to a database failure.

- Controller: Handle application-level exceptions and return appropriate HTTP responses.
- Service: Handle business logic exceptions and coordinate between layers.
- Repository: Handle data access exceptions and propagate them to the service layer.

```java
@Controller
public class OrderController {
    @PostMapping("/orders")
    public ResponseEntity<?> createOrder(@RequestBody OrderRequest request) {
        try {
            orderService.placeOrder(request);
            return ResponseEntity.ok("Order created successfully");
        } catch (InvalidOrderException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (OrderProcessingException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Order processing failed");
        }
    }
}

@Service
class OrderService {
    private final PaymentGateway paymentGateway;

    OrderService(PaymentGateway paymentGateway) {
        this.paymentGateway = paymentGateway;
    }

    public void placeOrder(OrderRequest request) {
        if (request.items().isEmpty()) {
            throw new InvalidOrderException(
                "Order must contain items"
            );
        }

        try {
            paymentGateway.charge(request);
        } catch (PaymentGatewayException e) {
            throw new OrderProcessingException(
                "Unable to process order payment", e
            );
        }
    }
}

```

#### Log once at the appropriate boundary

A common anti-pattern is logging and rethrowing the same exception at every layer.

**Bad Practise**

```java
catch (IOException e) {
    logger.error("Failed to read data", e);
    throw new DataAccessException("Failed", e);
}
```

**Good Practise**

```java
// At the lower layer
catch (IOException e) {
    throw new DataAccessException(
        "Unable to load customer data", e
    );
}

// At the application boundary (controller/service)
catch (DataAccessException e) {
    logger.error(
        "Customer request failed, requestId={}",
        requestId,
        e
    );
}
```

!!! info "Key Takeaways"
    Prefer logging exceptions at a boundary where you have sufficient context and decide the final outcome. Intermediate layers should generally propagate or translate exceptions without duplicating logs.

#### Use try-with-resources for resource cleanup

Use try-with-resources for objects implementing `AutoCloseable` to ensure proper resource management and cleanup.

```java

public String readFile(Path path) throws IOException {
    try (BufferedReader reader =
             Files.newBufferedReader(path)) {

        return reader.readLine();
    }
}
```

#### Handle `InterruptedException` correctly

This is particularly important for multithreaded Java applications. When you cannot propagate the checked exception, you should restore the thread's interrupt status by calling `Thread.currentThread().interrupt()` so higher level code can observe it. Returning also stops the current operation.

When an exception `InterruptedException` happens, the `Thread.currentThread().isInterrupted()` status changes to `true`. However, when catching `InterruptedException`, the JVM automatically clears the interrupt status (resets it from `true` to `false`).

If your method can declare `throws InterruptedException`, do so instead of catching it.

```java
try {
    Thread.sleep(1000);
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    return;
}
```

#### Don't use exceptions for normal control flow

Use return values to represent expected outcomes. Use exceptions to represent failures that prevent an operation from fulfilling its contract.

**Normal Control Flow** represents expected pahts through an application, such as:

- A user exists or doesn't exist when searching
- A product matches or doesn't match a filter.
- A queue has an item or is empty.
- A validation succeeds or fails.
- A customer qualifies or doesn't qualify for a promotion.

These outcomes aren't necessarily application failures.

**Bad Example**: Using an exception to find a user.

```java
public User findUser(long id) {
    try {
        return repository.getById(id);
    } catch (UserNotFoundException e) {
        return null;
    }
}
```

This approach has several problems.

First, it treats a potentially expected absence as an exceptional event.

Second, the caller receives `null` without a clear contract.

Third, creating exceptions and collecting stack traces can introduce unnessary overhead, especially in frequently executed paths.

Also, this example assumes `getById()` immediatelly throws when the user is missing; not every repository API behaves that way.

**Better Example**: Use `Optional`

```java
public Optional<User> findUser(long id) {
    return repository.findById(id);
}

// Caller
Optional<User> result = findUser(100);

if (result.isPresent()) {
    System.out.println(result.get().getName());
} else {
    System.out.println("User not found");
}

// Or even cleaner
findUser(100)
    .ifPresentOrElse(
        user -> System.out.println(user.getName()),
        () -> System.out.println("User not found")
    );

```

The exceptions are not ideal for expected branches because:

- Exceptions can obscure business logic

```java
// Bad: Exception-driven flow
try {
    applyDiscount(customer);
} catch (NotEligibleException e) {
    return originalPrice;
}

// Good: Explicit conditional flow

if (discountPolicy.isEligible(customer)) {
    return applyDiscount(customer);
}

return originalPrice;
```

- Exceptions can have additional runtime cost: Constructing an exception typically captures stack-trace information, which can be more expensive than a simple conditional or return value. However, don't assume exceptions are always a measurable performance problem. JVM optimizations, exception construction, and the frequency of the failure path all matter. Measure before making performance claims.

- Exceptions make contracts harder to understand
- Exception handling can become accidental control flow

##### Alternatives

- Use `boolean` for simple decisions
- Use `Optional` for expected absence
- Use `Result` or `Either` for error handling
- Use Sealed result types

A simple result object can represent success and failure, but it may allow invalid combinations of fields.

For example, `valid = true` while `errorCode=INVALID_EMAIL`

Sealed types provide stronger modeling

```java
sealed interface RegistrationResult
        permits RegistrationResult.Success,
                RegistrationResult.InvalidEmail,
                RegistrationResult.AlreadyExists {

    record Success(long userId)
            implements RegistrationResult {}

    record InvalidEmail(String reason)
            implements RegistrationResult {}

    record AlreadyExists(String email)
            implements RegistrationResult {}
}
```

Now the possible outcomes are explicitly represented as different types.

```java
public RegistrationResult register(String email) {
    if (email == null || !email.contains("@")) {
        return new RegistrationResult.InvalidEmail(
            "Invalid email format"
        );
    }

    if (repository.existsByEmail(email)) {
        return new RegistrationResult.AlreadyExists(email);
    }

    User user = repository.save(new User(email));

    return new RegistrationResult.Success(user.getId());
}
```

The caller can handle each result using Java 21 pattern matching for `switch`

```java
RegistrationResult result = register(email);

String message = switch (result) {
    case RegistrationResult.Success s ->
        "Created user " + s.userId();

    case RegistrationResult.InvalidEmail e ->
        "Validation failed: " + e.reason();

    case RegistrationResult.AlreadyExists e ->
        "Already registered: " + e.email();
};
```

The compiler can check whether the switch covers all permitted result types. This is a major advantage when modeling multiple expected outcomes.

**Important distinction between Business rejection vs Infrastructure failure**

Consider a bank transfer:

```java
sealed interface TransferResult {
    record Success(String transferId)
            implements TransferResult {}

    record InsufficientFunds()
            implements TransferResult {}

    record LimitExceeded()
            implements TransferResult {}
}
```

Think of it this way:

- **Business rejections** (like `InsufficientFunds` or `LimitExceeded`) are expected outcomes that the application should handle gracefully.
- **Infrastructure failures** (like network timeouts or database connection issues) are exceptional conditions that should be propagated as exceptions.

Therefore, a database outage should not return `InsufficientFunds`, `false`, or `Optional.empty()`. That would falsely communicate a business outcome.

Also, a timeout after submitting a transfer can mean the outcome is unknown, not necessarily failed. It may require a separate pending or unknown state and reconcilation.

##### Code review

```java
public boolean transfer(
        long fromAccount,
        long toAccount,
        BigDecimal amount) {

    try {
        accountService.withdraw(fromAccount, amount);
        accountService.deposit(toAccount, amount);
        return true;

    } catch (InsufficientFundsException e) {
        return false;

    } catch (Exception e) {
        return false;
    }
}
```

??? question "What's wrong with this code?"
    The problems are that false loses the reason for failure, broad exception handling hides infrastructure errors, and the withdrawal might succeed while the deposit fails.

    I would model expected business rejections with a TransferResult, propagate unexpected technical failures, and ensure both balance changes happen atomically within a database transaction when they share the same transactional database.

    If accounts are managed by separate services, a single local database transaction is insufficient. I would design an explicit distributed workflow with durable state, idempotency, and recovery or compensation.

    Key insight: Choosing a return type is only one part of error handling. Correctness also depends on transactional boundaries and failure recovery.

```java
public void processOrder(Order order) {
    try {
        paymentGateway.charge(order);
        orderRepository.save(order);
    } catch (Exception e) {
        logger.error("Order failed", e);
        throw new RuntimeException(e);
    }
}
```

??? question "What's wrong with this code?"
    Key improvements

    1. Catch known payment or persistence exceptions rather than every Exception.
    2. Preserve the original cause and use meaningful exception types.
    3. Avoid duplicate logging if the boundary already logs the failure.
    4. Do not assume a failed order means payment was unsuccessful.
    5. Persist order/payment state and use an appropriate recovery workflow, such as reconciliation, idempotent retries, or a Saga with compensation.

    **Important**: Adding @Transactional alone would not solve the problem because a database transaction cannot automatically roll back an external payment.

#### Avoid exposing sensitive information

Exposing sensitive data—such as internal server file paths, IP addresses, database schemas, PII (Personally Identifiable Information), or encryption keys—via exception logs or error responses is a major security vulnerability known as Information Disclosure.

Here is how to properly manage exceptions while keeping your application secure:

##### Never Leak Details to the End User or Client

An attacker can intentionally trigger exceptions (e.g sending bad input to cause timeouts or interrupts) to map out your infrastructure. Always map internal exceptions to clean, generic user-facing messages.

```java
// Bad: Exposing technical stack and internal details to the API response
@GetMapping("/process")
public ResponseEntity<String> process() {
    try {
        taskService.execute();
        return ResponeEntity.ok("Success");
    } catch (Exception e) {
        return ResponseEntity.status(500).body("Error: Thread " + Thread.currentThread().getName() + " was interrupted. Stack: " + e.getMessage());
    }
}

// Good: Return a generic message to the user, log the details securely inside the server
@GetMapping("/process")
public ResponseEntity<String> process() {
    try {
        taskService.execute();
        return ResponeEntity.ok("Success");
    } catch (Exception e) {
        // Log the details securely (e.g., to a monitoring system)
        logger.error("Error processing task", e);
        return ResponseEntity.status(500).body("An unexpected error occurred. Please try again later.");
    }
}
```

##### Sanitize Server Logs (Prevent Log Injection & PII Leaks)

Even inside internal server logs, exposing raw parameters can violate compliance rules. If a thread gets interrupted while processing a specific user's transaction, do not dump the raw transaction details straight into the log message.

```java
// Bad: Exposing raw transaction details in logs
logger.error("Error processing transaction for user {}: {}", userId, transactionDetails);

// Good: Mask sensitive data in logs
logger.error("Error processing transaction for user {}: {}", userId, "MASKED");
```

##### Mask Stack Traces in Production

Ensure your application framework is configured to **disable stack traces** in HTTP error responses.

In your application.properties or application.yml file, always ensure production environments explicitly mask these details:

```text
server.error.include-stacktrace=never
server.error.include-message=never
```

### Common Java Errors

Represents serious failures, often involving the JVM or runtime environment.

There are some common Java Errors.

#### OutOfMemoryError

Occurs when the JVM cannot allocate the required memory.

```java
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        List<byte[]> data = new ArrayList<>();

        while (true) {
            data.add(new byte[1024 * 1024]);
        }
    }
}
```

This example deliberately retains large arrays until memory exhaustion. Do not run it in a normal application environment.

Typical causes include memory leaks, retaining too many objects, insufficient heap capacity, or excessive allocation.

Possible responses include investigating heap dumps, reviewing object retention, and adjusting memory limits where appropriate.

#### StackOverflowError

Occurs when a thread exhausts its stack, commonly due to excessive recursion.

```java
public class Main {
    static void recursive() {
        recursive();
    }

    public static void main(String[] args) {
        recursive();
    }
}
```

The method never reaches a termination condition.

A common solution is to fix the recursion logic or replace deep recursion with an iterative approach.

#### NoClassDefFoundError

Occurs when a class that was available at compilation or earlier resolution cannot be found or initialized when the JVM needs it.

Possible causes include a missing runtime dependency, incorrect classpath, or a previous claas initialization failure.

**Important distinction** `ClassNotFoundException` is a checked exception commonly associated with explicit class loading, while `NoClassDefFoundError` is an unchecked `Error` associated with class resolution or initialization.

### What should/shouldn't catch exceptions

- Catch exceptions that you can handle meaningfully
- Don't catch exceptions that you can't handle
- Don't catch exceptions that you don't understand
- Techincally, we can catch `Error` but it's not recommended. Catching an Error does not mean the application can safely recover.
- Don't catch `Throwable` directly, catch `Exception` or specific exception types. Because `Throwable` includes both `Error` and `Exception`.

In Java, both Exception and Error inherit from Throwable.

Exception represents conditions that an application may handle, such as IOException, SQLException, or IllegalArgumentException. Exceptions can be checked or unchecked.

Error represents serious failures, often related to the JVM, runtime environment, or class loading. Examples include OutOfMemoryError, StackOverflowError, and NoClassDefFoundError.

Although Java allows us to catch Error, we generally should not because the application may not be in a safe state to recover.

In production, I prefer catching specific exceptions, logging meaningful diagnostic information, and using monitoring and process recovery mechanisms for serious runtime failures.

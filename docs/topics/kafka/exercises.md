# Kafka Hands-On Exercises

Practical engineering exercises to strengthen your understanding of Kafka message flow, idempotency, and error handling.

---

## Exercise 1: Custom Idempotent Deduplication Record Interceptor

### Objective

Build a reusable Spring Kafka `RecordInterceptor` that automatically screens incoming consumer records for duplicate deliveries using an atomic deduplication store before the listener method is invoked.

### Requirements

1. Intercept every incoming `ConsumerRecord<K, V>`.
2. Extract a unique event identifier from record headers (`X-Event-Id`) or message key.
3. Query an atomic deduplication store:
   - If already processed: Log a warning, commit the offset immediately via `acknowledgment.acknowledge()`, and return `null` so the `@KafkaListener` method body is never executed.
   - If not processed: Allow the record to pass through to the listener.
4. Add unit tests asserting duplicate suppression and first-time pass-through.

### Verification Checklist

- [ ] `RecordInterceptor.intercept(ConsumerRecord, Consumer)` returns `null` for duplicate IDs.
- [ ] Valid first-time records proceed to the listener unchanged.
- [ ] Offset is committed so the duplicate is not re-polled indefinitely.

---

## Exercise 2: Exponential Backoff Dead Letter Topic Routing with Error Classification

### Objective

Implement and configure a production-grade `DefaultErrorHandler` with custom exception classification and Dead Letter Topic routing.

### Requirements

1. Configure an `ExponentialBackOff` starting at 1,000ms with multiplier 2.0 and max 3 attempts.
2. Configure a `DeadLetterPublishingRecoverer` routing failed messages to `<topic-name>.CUSTOM_DLT`.
3. Add custom non-retryable exception classification:
   - Mark `IllegalArgumentException`, `MethodArgumentNotValidException`, and `org.apache.kafka.common.errors.SerializationException` as fatal.
   - Ensure fatal exceptions bypass retries and publish directly to the DLT on the first failure.
4. Verify behavior using `SharedKafkaContainer` or unit tests with Mockito.

### Verification Checklist

- [ ] Transient exceptions retry 3 times with exponential delays before DLT publication.
- [ ] Fatal validation exceptions route immediately to `.CUSTOM_DLT` on attempt 1.
- [ ] Original exception message and stack trace are preserved in Kafka record headers.

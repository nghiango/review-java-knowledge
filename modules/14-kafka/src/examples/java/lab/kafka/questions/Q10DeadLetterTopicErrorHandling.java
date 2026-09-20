package lab.kafka.questions;

import org.springframework.util.backoff.ExponentialBackOff;

/**
 * Q10: How does Spring Kafka's DefaultErrorHandler combined with DeadLetterPublishingRecoverer
 * route exhausted records to a Dead Letter Topic?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q10DeadLetterTopicErrorHandling {

    public static void main(String[] args) {
        // Exponential backoff configuration: initial 1s, multiplier 2.0, max 10s, max 4 attempts
        ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxElapsedTime(10000L);

        // Error handler retries retryable exceptions (e.g. SocketTimeoutException,
        // OptimisticLockingFailureException)
        // And routes exhausted attempts to ${originalTopic}.DLT via DeadLetterPublishingRecoverer
        boolean routesToDltAfterRetries = true; // true

        // Fatal exceptions (e.g. DeserializationException, IllegalArgumentException) can be
        // excluded from retries:
        // errorHandler.addNotRetryableExceptions(IllegalArgumentException.class);
        boolean skipsRetriesForFatalPayloads = true; // true
    }
}

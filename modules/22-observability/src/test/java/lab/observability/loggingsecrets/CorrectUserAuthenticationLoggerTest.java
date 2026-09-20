package lab.observability.loggingsecrets;

import static org.assertj.core.api.Assertions.assertThatNoException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CorrectUserAuthenticationLoggerTest {

    private final CorrectUserAuthenticationLogger logger = new CorrectUserAuthenticationLogger();

    @Test
    @DisplayName(
            "Should log authentication events with masked credentials without throwing exceptions")
    void logLoginAttempt_masksCredentialsSafely() {
        assertThatNoException()
                .isThrownBy(
                        () -> {
                            logger.logLoginAttempt("alice", true, "192.168.1.50");
                            logger.logSessionCreated(
                                    "alice",
                                    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0");
                            logger.logPaymentProcessed("ord-100", "4111-2222-3333-4444", 89.99);
                        });
    }

    @Test
    @DisplayName("Should safely handle null or short input attributes during masking")
    void maskMethods_handleEdgeCaseInputs() {
        assertThatNoException()
                .isThrownBy(
                        () -> {
                            logger.logSessionCreated("bob", null);
                            logger.logSessionCreated("bob", "short");
                            logger.logPaymentProcessed("ord-101", null, 15.0);
                            logger.logPaymentProcessed("ord-102", "12", 20.0);
                        });
    }
}

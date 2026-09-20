package lab.observability.broken.loggingsecrets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UserAuthenticationLogger {

    private static final Logger log = LoggerFactory.getLogger(UserAuthenticationLogger.class);

    public void logLoginAttempt(String username, String password, String clientIp) {
        // String concatenation with plain-text credentials
        log.info("User login attempt: username=" + username + ", password=" + password + ", ip=" + clientIp);
    }

    public void logSessionCreated(String username, String bearerToken) {
        log.info("User authenticated: " + username + " with token=" + bearerToken);
    }

    public void logPaymentProcessed(String orderId, String cardNumber, String cvv, double amount) {
        log.debug("Processed payment for order " + orderId + " using card " + cardNumber + " cvv=" + cvv + " amount=" + amount);
    }
}

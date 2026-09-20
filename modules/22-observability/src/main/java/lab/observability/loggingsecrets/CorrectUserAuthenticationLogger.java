package lab.observability.loggingsecrets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CorrectUserAuthenticationLogger {

    private static final Logger log =
            LoggerFactory.getLogger(CorrectUserAuthenticationLogger.class);

    public void logLoginAttempt(String username, boolean success, String clientIp) {
        // Redacts raw passwords entirely; logs only username, status, and parameterized client IP
        log.info(
                "User login attempt username={} success={} clientIp={}",
                username,
                success,
                clientIp);
    }

    public void logSessionCreated(String username, String bearerToken) {
        // Redacts bearer token to avoid session hijacking; exposes only masked key footprint
        String maskedToken = maskToken(bearerToken);
        log.info("User session established username={} tokenFootprint={}", username, maskedToken);
    }

    public void logPaymentProcessed(String orderId, String cardNumber, double amount) {
        // PCI-DSS compliance: CVV is never passed/logged; PAN is strictly masked to last 4 digits
        String maskedCard = maskCardNumber(cardNumber);
        log.info(
                "Payment processed orderId={} maskedCard={} amount={}",
                orderId,
                maskedCard,
                amount);
    }

    private String maskToken(String token) {
        if (token == null || token.length() <= 8) {
            return "***";
        }
        return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
    }

    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        String clean = cardNumber.replaceAll("\\s+|-", "");
        if (clean.length() < 4) {
            return "****";
        }
        String lastFour = clean.substring(clean.length() - 4);
        return "****-****-****-" + lastFour;
    }
}

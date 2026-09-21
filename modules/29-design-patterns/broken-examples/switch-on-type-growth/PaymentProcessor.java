package lab.designpatterns.broken.switchontype;

import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PaymentProcessor {

    private static final Logger log = LoggerFactory.getLogger(PaymentProcessor.class);

    public boolean processPayment(PaymentRequest request) {
        log.info("Processing payment for transaction {}", request.transactionId());

        switch (request.paymentType()) {
            case CREDIT_CARD:
                if (request.paymentDetails() == null || request.paymentDetails().length() < 16) {
                    throw new IllegalArgumentException("Invalid credit card format");
                }
                log.info("Contacting Visa/MasterCard gateway for {}", request.amount());
                return true;

            case DEBIT_CARD:
                if (!request.paymentDetails().startsWith("DEBIT-")) {
                    throw new IllegalArgumentException("Invalid debit card format");
                }
                log.info("Contacting Interac/Direct Debit network for {}", request.amount());
                return true;

            case PAYPAL:
                if (!request.paymentDetails().contains("@")) {
                    throw new IllegalArgumentException("Invalid PayPal email address");
                }
                log.info("Creating PayPal checkout session for {}", request.amount());
                return true;

            case CRYPTO:
                if (!request.paymentDetails().startsWith("0x")) {
                    throw new IllegalArgumentException("Invalid wallet address");
                }
                log.info("Checking blockchain confirmations for {}", request.amount());
                return true;

            case BANK_TRANSFER:
                if (request.paymentDetails().length() != 10) {
                    throw new IllegalArgumentException("Invalid IBAN length");
                }
                log.info("Initiating SEPA transfer for {}", request.amount());
                return true;

            default:
                throw new UnsupportedOperationException("Unknown payment type: " + request.paymentType());
        }
    }

    public BigDecimal calculateFee(PaymentRequest request) {
        switch (request.paymentType()) {
            case CREDIT_CARD:
                return request.amount().multiply(new BigDecimal("0.029")).add(new BigDecimal("0.30"));
            case DEBIT_CARD:
                return new BigDecimal("0.25");
            case PAYPAL:
                return request.amount().multiply(new BigDecimal("0.034")).add(new BigDecimal("0.35"));
            case CRYPTO:
                return new BigDecimal("0.0005");
            case BANK_TRANSFER:
                return BigDecimal.ZERO;
            default:
                throw new UnsupportedOperationException("Unknown payment type: " + request.paymentType());
        }
    }

    public void refund(PaymentRequest request) {
        switch (request.paymentType()) {
            case CREDIT_CARD:
                log.info("Issuing credit card chargeback reversal");
                break;
            case DEBIT_CARD:
                log.info("Reversing direct debit entry");
                break;
            case PAYPAL:
                log.info("Triggering PayPal refund API");
                break;
            case CRYPTO:
                throw new UnsupportedOperationException("Crypto payments cannot be automatically refunded");
            case BANK_TRANSFER:
                log.info("Queueing wire transfer return");
                break;
            default:
                throw new UnsupportedOperationException("Unknown payment type: " + request.paymentType());
        }
    }
}

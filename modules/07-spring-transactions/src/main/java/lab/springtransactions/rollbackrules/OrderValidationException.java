package lab.springtransactions.rollbackrules;

public class OrderValidationException extends Exception {

    public OrderValidationException(String message) {
        super(message);
    }
}

package lab.springtransactions.broken.checkedexception;

public class OrderValidationException extends Exception {

    public OrderValidationException(String message) {
        super(message);
    }
}

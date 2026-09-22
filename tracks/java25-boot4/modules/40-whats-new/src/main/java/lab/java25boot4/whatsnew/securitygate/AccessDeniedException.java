package lab.java25boot4.whatsnew.securitygate;

/** Raised when a caller is not permitted to perform an operation. */
public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException(String message) {
        super(message);
    }
}

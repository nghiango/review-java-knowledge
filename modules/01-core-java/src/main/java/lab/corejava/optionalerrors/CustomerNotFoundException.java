package lab.corejava.optionalerrors;

public final class CustomerNotFoundException extends RuntimeException {
    private final String email;

    public CustomerNotFoundException(String email) {
        super("Customer profile not found for email: " + email);
        this.email = email;
    }

    public String email() {
        return email;
    }
}

package lab.springmvc.broken.inputvalidation;

public class CreateUserRequest {

    private String username;
    private String email;
    private double initialBalance;

    public CreateUserRequest() {}

    public CreateUserRequest(String username, String email, double initialBalance) {
        this.username = username;
        this.email = email;
        this.initialBalance = initialBalance;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public double getInitialBalance() {
        return initialBalance;
    }

    public void setInitialBalance(double initialBalance) {
        this.initialBalance = initialBalance;
    }
}

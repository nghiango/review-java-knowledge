package lab.springmvc.broken.inputvalidation;

import org.springframework.stereotype.Service;

@Service
public class UserService {

    public String registerUser(CreateUserRequest request) {
        if (request.getUsername() == null) {
            throw new IllegalArgumentException("Username required");
        }
        return "User created: " + request.getUsername() + ", balance: " + request.getInitialBalance();
    }
}
